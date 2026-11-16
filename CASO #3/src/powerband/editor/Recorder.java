package powerband.editor;

import static powerband.util.IConstants.EMPTY_RECORDING_NAME;
import static powerband.util.IConstants.ERR_ALREADY_RECORDING;
import static powerband.util.IConstants.ERR_INVALID_NAME;
import static powerband.util.IConstants.ERR_NOT_RECORDING;
import static powerband.util.IConstants.ERR_NO_PENDING_RECORDING;
import static powerband.util.IConstants.ERR_NULL_COMMAND;
import static powerband.util.IConstants.ERR_NULL_INDEXER;
import static powerband.util.IConstants.ERR_NULL_STORAGE;
import static powerband.util.IConstants.PENDING_POSITION;
import static powerband.util.IConstants.RECORDING_MIN_TIME_MS;
import static powerband.util.IConstants.RECORD_SIZE;

import powerband.command.ISynthCommand;
import powerband.command.ISynthEventListener;
import powerband.command.OrganCommand;
import powerband.exception.DuplicateRecordingNameException;
import powerband.exception.StorageException;
import powerband.exception.SynthConnectionException;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.model.SynthEvent;
import powerband.storage.IRecordingIndexer;
import powerband.storage.RecordingStorage;

/**
 * Grabador del editor: escucha los 3 comandos y, mientras graba, escribe cada
 * evento de inmediato en recordings.music con su tiempo relativo al inicio.
 *
 * <p>Flujo: {@link #start()} → eventos → {@link #stop()} → {@link #save(String)}
 * (se puede reintentar si el nombre está repetido) o {@link #discard()}.</p>
 *
 * <p>Todos los métodos son {@code synchronized}: los eventos llegan desde los
 * hilos de los instrumentos.</p>
 */
public class Recorder implements ISynthEventListener {

    private enum State {
        IDLE, RECORDING, PENDING_NAME
    }

    private final RecordingStorage storage;
    private final IRecordingIndexer indexer;
    private final OrganCommand organ;

    private State state = State.IDLE;
    private long headerPosition;
    private long endPosition;
    private long startTimeMs;
    private StorageException writeFailure; // primera falla dentro de onSynthEvent

    public Recorder(RecordingStorage pStorage, IRecordingIndexer pIndexer, ISynthCommand pPiano,
            ISynthCommand pBass, OrganCommand pOrgan) {
        if (pStorage == null) {
            throw new IllegalArgumentException(ERR_NULL_STORAGE);
        }
        if (pIndexer == null) {
            throw new IllegalArgumentException(ERR_NULL_INDEXER);
        }
        if (pPiano == null || pBass == null || pOrgan == null) {
            throw new IllegalArgumentException(ERR_NULL_COMMAND);
        }
        storage = pStorage;
        indexer = pIndexer;
        organ = pOrgan;
        pPiano.addListener(this);
        pBass.addListener(this);
        pOrgan.addListener(this);
    }

    /** Escribe el encabezado provisional y empieza a grabar. */
    public synchronized void start() throws StorageException {
        if (state != State.IDLE) {
            throw new IllegalStateException(ERR_ALREADY_RECORDING);
        }
        headerPosition = storage.musicLength();
        storage.appendHeader(new RecordingHeader(EMPTY_RECORDING_NAME,
                headerPosition + RECORD_SIZE, PENDING_POSITION));
        writeFailure = null;
        startTimeMs = System.currentTimeMillis();
        state = State.RECORDING;
    }

    /**
     * Apaga las notas encendidas del órgano (sus noteOff se graban) y deja la
     * grabación pendiente de nombre.
     *
     * @throws StorageException si alguna escritura falló durante la grabación
     */
    public synchronized void stop() throws StorageException, SynthConnectionException {
        if (state != State.RECORDING) {
            throw new IllegalStateException(ERR_NOT_RECORDING);
        }
        try {
            // Todavía en RECORDING y con el lock: los noteOff vuelven por onSynthEvent.
            organ.allNotesOff();
        } finally {
            state = State.PENDING_NAME;
            endPosition = storage.musicLength();
        }
        if (writeFailure != null) {
            throw writeFailure;
        }
    }

    /**
     * Cierra la grabación pendiente con ese nombre.
     *
     * @throws IllegalArgumentException si el nombre no es válido
     * @throws DuplicateRecordingNameException si el nombre ya existe; se puede reintentar
     */
    public synchronized void save(String pName)
            throws StorageException, DuplicateRecordingNameException {
        if (state != State.PENDING_NAME) {
            throw new IllegalStateException(ERR_NO_PENDING_RECORDING);
        }
        if (!RecordingStorage.isValidName(pName)) {
            throw new IllegalArgumentException(ERR_INVALID_NAME + pName);
        }
        if (storage.listContainsName(pName)) {
            throw new DuplicateRecordingNameException(pName);
        }
        storage.writeHeader(headerPosition,
                new RecordingHeader(pName, headerPosition + RECORD_SIZE, endPosition));
        storage.appendListEntry(new ListEntry(pName, System.currentTimeMillis()));
        // La grabación ya quedó en disco y en la lista: aunque falle el índice no
        // puede seguir pendiente, porque un discard la recortaría.
        state = State.IDLE;
        indexer.add(pName, headerPosition);
    }

    /** Descarta la grabación pendiente: recorta el archivo a donde estaba antes de start. */
    public synchronized void discard() throws StorageException {
        if (state != State.PENDING_NAME) {
            throw new IllegalStateException(ERR_NO_PENDING_RECORDING);
        }
        storage.truncateMusic(headerPosition);
        state = State.IDLE;
    }

    @Override
    public synchronized void onSynthEvent(SynthEvent pEvent) {
        if (state != State.RECORDING || writeFailure != null || pEvent == null) {
            return;
        }
        long relativeMs = Math.max(RECORDING_MIN_TIME_MS, pEvent.getTimeMs() - startTimeMs);
        try {
            storage.appendEvent(pEvent.withTimeMs(relativeMs));
        } catch (StorageException e) {
            writeFailure = e; // este método no puede lanzarla; stop la relanza
        }
    }

    public synchronized boolean isRecording() {
        return state == State.RECORDING;
    }

    public synchronized boolean isPendingName() {
        return state == State.PENDING_NAME;
    }

    public synchronized long getStartTimeMs() {
        return startTimeMs;
    }
}
