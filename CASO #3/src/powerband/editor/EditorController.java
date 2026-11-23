package powerband.editor;

import static powerband.util.IConstants.ERR_INVALID_STATE;
import static powerband.util.IConstants.ERR_NOTHING_LOADED;
import static powerband.util.IConstants.ERR_NULL_EDITOR_DEPENDENCY;
import static powerband.util.IConstants.ERR_SHUTDOWN_STEP;
import static powerband.util.IConstants.LIST_DATE_PATTERN;
import static powerband.util.IConstants.LIST_LINE_FORMAT;
import static powerband.util.IConstants.MONITOR_DUR_SUFFIX;
import static powerband.util.IConstants.MONITOR_LINE_FORMAT;
import static powerband.util.IConstants.MONITOR_NO_VALUE;
import static powerband.util.IConstants.MONITOR_TIME_PATTERN;
import static powerband.util.IConstants.STATUS_FAILED;
import static powerband.util.IConstants.STATUS_NONE;
import static powerband.util.IConstants.STATUS_PART_SEPARATOR;
import static powerband.util.IConstants.STATUS_REASON_CLOSE;
import static powerband.util.IConstants.STATUS_REASON_OPEN;
import static powerband.util.IConstants.STATUS_SEPARATOR;
import static powerband.util.IConstants.STATUS_STARTED;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import powerband.command.OrganCommand;
import powerband.exception.DuplicateRecordingNameException;
import powerband.exception.PowerBandException;
import powerband.exception.RecordingNotFoundException;
import powerband.exception.StorageException;
import powerband.exception.SynthConnectionException;
import powerband.instrument.Instrument;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.storage.RecordingStorage;

/**
 * Lógica del editor, sin Swing: coordina grabador, reproductor, biblioteca e
 * instrumentos y aplica las reglas de estado. No se puede grabar mientras se
 * reproduce (se grabaría la reproducción) ni reproducir mientras se graba.
 *
 * <p>El estado es {@code synchronized} porque el fin de una reproducción llega
 * desde el hilo del {@link Player}. La vista se avisa dentro del lock para que los
 * avisos lleguen en el mismo orden que los cambios (una reproducción vacía puede
 * terminar antes de que {@link #play()} regrese); por eso {@link IEditorView} no
 * debe bloquear.</p>
 */
public class EditorController implements IPlaybackListener {

    private static final DateTimeFormatter MONITOR_TIME =
            DateTimeFormatter.ofPattern(MONITOR_TIME_PATTERN).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter LIST_DATE =
            DateTimeFormatter.ofPattern(LIST_DATE_PATTERN).withZone(ZoneId.systemDefault());

    private final Recorder recorder;
    private final Player player;
    private final RecordingLibrary library;
    private final OrganCommand organ;
    private final Map<String, Instrument> instruments = new LinkedHashMap<>();
    private final List<Instrument> started = new ArrayList<>();
    private final Map<String, String> failed = new LinkedHashMap<>(); // etiqueta -> motivo

    private EditorState state = EditorState.IDLE;
    private RecordingHeader loaded;
    private boolean closed;
    private volatile IEditorView view;

    public EditorController(Recorder pRecorder, Player pPlayer, RecordingLibrary pLibrary,
            OrganCommand pOrgan) {
        if (pRecorder == null || pPlayer == null || pLibrary == null || pOrgan == null) {
            throw new IllegalArgumentException(ERR_NULL_EDITOR_DEPENDENCY);
        }
        recorder = pRecorder;
        player = pPlayer;
        library = pLibrary;
        organ = pOrgan;
        player.setListener(this);
    }

    public void setView(IEditorView pView) {
        view = pView;
    }

    // ------------------------------------------------------------ instrumentos

    public synchronized void addInstrument(String pLabel, Instrument pInstrument) {
        instruments.put(pLabel, pInstrument);
    }

    /** Inicia cada instrumento; si uno falla, guarda el motivo y sigue con los demás. */
    public synchronized void startInstruments() {
        for (Map.Entry<String, Instrument> entry : instruments.entrySet()) {
            try {
                entry.getValue().start();
                started.add(entry.getValue());
            } catch (PowerBandException e) {
                failed.put(entry.getKey(), e.getMessage());
            }
        }
    }

    /** "Iniciados: a, b  |  Fallaron: c (motivo)". */
    public synchronized String getInstrumentStatus() {
        List<String> startedLabels = new ArrayList<>();
        for (Map.Entry<String, Instrument> entry : instruments.entrySet()) {
            if (started.contains(entry.getValue())) {
                startedLabels.add(entry.getKey());
            }
        }
        List<String> failedLabels = new ArrayList<>();
        for (Map.Entry<String, String> entry : failed.entrySet()) {
            failedLabels.add(entry.getKey() + STATUS_REASON_OPEN + entry.getValue()
                    + STATUS_REASON_CLOSE);
        }
        return STATUS_STARTED + join(startedLabels) + STATUS_PART_SEPARATOR
                + STATUS_FAILED + join(failedLabels);
    }

    // ------------------------------------------------------------ grabación

    public synchronized EditorState getState() {
        return state;
    }

    public synchronized void startRecording() throws StorageException {
        require(EditorState.IDLE);
        recorder.start();
        changeState(EditorState.RECORDING);
    }

    /** Detiene la grabación; queda esperando nombre aunque una escritura haya fallado. */
    public synchronized void stopRecording() throws StorageException, SynthConnectionException {
        require(EditorState.RECORDING);
        try {
            recorder.stop();
        } finally {
            if (recorder.isPendingName()) {
                changeState(EditorState.NAMING);
            }
        }
    }

    /**
     * Guarda la grabación pendiente con ese nombre.
     *
     * @throws IllegalArgumentException si el nombre no es válido; sigue esperando nombre
     * @throws DuplicateRecordingNameException si ya existe; sigue esperando nombre
     */
    public synchronized void saveRecording(String pName)
            throws StorageException, DuplicateRecordingNameException {
        require(EditorState.NAMING);
        try {
            recorder.save(pName);
        } finally {
            if (!recorder.isPendingName()) {
                // también si el índice falló: la grabación ya está en la lista
                changeState(EditorState.IDLE);
                IEditorView current = view;
                if (current != null) {
                    current.recordingsChanged();
                }
            }
        }
    }

    public synchronized void discardRecording() throws StorageException {
        require(EditorState.NAMING);
        recorder.discard();
        changeState(EditorState.IDLE);
    }

    // ------------------------------------------------------------ carga y reproducción

    /**
     * Busca la grabación por nombre (búsqueda binaria en el índice) y la deja cargada.
     * Si no existe, la grabación cargada antes no cambia.
     */
    public synchronized void load(String pName)
            throws StorageException, RecordingNotFoundException {
        require(EditorState.IDLE);
        loaded = library.load(pName);
    }

    /** Nombre de la grabación cargada, o {@code null}. */
    public synchronized String getLoadedName() {
        return loaded == null ? null : loaded.getName();
    }

    /** Toca la grabación cargada en el hilo del reproductor. */
    public synchronized void play() throws StorageException {
        require(EditorState.IDLE);
        if (loaded == null) {
            throw new IllegalStateException(ERR_NOTHING_LOADED);
        }
        // Se cambia antes de start: el hilo del reproductor ya puede avisar que terminó.
        changeState(EditorState.PLAYING);
        try {
            player.start(loaded);
        } catch (RuntimeException e) {
            changeState(EditorState.IDLE);
            throw e;
        }
    }

    /**
     * Detiene la reproducción. No toma el lock mientras espera al reproductor:
     * su hilo llama {@link #onPlaybackFinished}, que necesita ese lock.
     */
    public void stopPlayback() {
        player.stop();
    }

    @Override
    public synchronized void onPlaybackFinished(String pName) {
        if (state == EditorState.PLAYING) {
            changeState(EditorState.IDLE);
        }
    }

    // ------------------------------------------------------------ lista

    public long recordingCount() throws StorageException {
        return library.listCount();
    }

    public ListEntry recordingAt(long pIndex) throws StorageException {
        return library.listEntry(pIndex);
    }

    public static boolean isValidName(String pName) {
        return RecordingStorage.isValidName(pName);
    }

    // ------------------------------------------------------------ cierre

    /**
     * Cierre ordenado: descarta la grabación sin terminar, detiene el reproductor y
     * los instrumentos y apaga el órgano. Nunca lanza: cada error va a la vista.
     */
    public void shutdown() {
        EditorState current;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            current = state;
        }
        if (current == EditorState.RECORDING) {
            try {
                recorder.stop();
            } catch (PowerBandException e) {
                reportShutdown(e);
            }
        }
        if (current == EditorState.RECORDING || current == EditorState.NAMING) {
            try {
                recorder.discard();
            } catch (PowerBandException | IllegalStateException e) {
                reportShutdown(e);
            }
        }
        player.stop(); // fuera del lock: ver stopPlayback
        List<Instrument> toStop;
        synchronized (this) {
            toStop = new ArrayList<>(started);
        }
        for (Instrument instrument : toStop) {
            try {
                instrument.stop();
            } catch (RuntimeException e) {
                reportShutdown(e);
            }
        }
        try {
            organ.allNotesOff();
        } catch (SynthConnectionException e) {
            reportShutdown(e);
        }
    }

    // ------------------------------------------------------------ formatos

    /** Línea del monitor: hora, synth, acción, nota y duración. */
    public static String formatEvent(SynthEvent pEvent) {
        String duration = pEvent.getAction() == SynthAction.TRIGGER
                ? pEvent.getDurationMs() + MONITOR_DUR_SUFFIX
                : MONITOR_NO_VALUE;
        return String.format(MONITOR_LINE_FORMAT,
                MONITOR_TIME.format(Instant.ofEpochMilli(pEvent.getTimeMs())),
                pEvent.getSynth().name(), pEvent.getAction().name(), pEvent.getNote(), duration);
    }

    /** Línea de la lista: nombre y fecha/hora de creación. */
    public static String formatListEntry(ListEntry pEntry) {
        return String.format(LIST_LINE_FORMAT, pEntry.getName(),
                LIST_DATE.format(Instant.ofEpochMilli(pEntry.getCreatedAtMs())));
    }

    // ------------------------------------------------------------ auxiliares

    private void require(EditorState pExpected) {
        if (state != pExpected) {
            throw new IllegalStateException(ERR_INVALID_STATE + state);
        }
    }

    /** Cambia el estado y avisa a la vista. Se llama con el lock tomado. */
    private void changeState(EditorState pState) {
        state = pState;
        IEditorView current = view;
        if (current != null) {
            current.stateChanged(pState);
        }
    }

    private void reportShutdown(Exception pError) {
        IEditorView current = view;
        if (current != null) {
            current.showError(ERR_SHUTDOWN_STEP + pError.getMessage());
        }
    }

    private static String join(List<String> pItems) {
        return pItems.isEmpty() ? STATUS_NONE : String.join(STATUS_SEPARATOR, pItems);
    }
}
