package powerband.editor;

import static powerband.util.IConstants.ERR_ALREADY_PLAYING;
import static powerband.util.IConstants.ERR_NULL_COMMAND;
import static powerband.util.IConstants.ERR_NULL_HEADER;
import static powerband.util.IConstants.ERR_NULL_STORAGE;
import static powerband.util.IConstants.ERR_PLAYER_READ;
import static powerband.util.IConstants.ERR_PLAYER_SEND;
import static powerband.util.IConstants.PLAYER_NO_WAIT_MS;
import static powerband.util.IConstants.PLAYER_STOP_TIMEOUT_MS;
import static powerband.util.IConstants.PLAYER_THREAD_NAME;
import static powerband.util.IConstants.RECORD_SIZE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import powerband.command.ISynthCommand;
import powerband.exception.StorageException;
import powerband.exception.SynthConnectionException;
import powerband.model.RecordingHeader;
import powerband.model.SynthEvent;
import powerband.storage.RecordingStorage;

/**
 * Reproduce una grabación con los mismos {@link ISynthCommand} que usan los
 * instrumentos (patrón Bridge): lee los comandos de disco uno por uno y envía cada
 * uno cuando toca según el inicio de la reproducción, para que suene igual que
 * cuando se grabó.
 */
public class Player {

    private final RecordingStorage storage;
    private final ISynthCommand piano;
    private final ISynthCommand bass;
    private final ISynthCommand organ;
    private final Object lifecycleLock = new Object();
    /** Notas del órgano encendidas por la reproducción actual; se apagan al terminar. */
    private final Set<Integer> heldOrganNotes = new HashSet<>();

    private volatile boolean playing;
    private volatile IPlaybackListener listener;
    private Thread thread;

    public Player(RecordingStorage pStorage, ISynthCommand pPiano, ISynthCommand pBass,
            ISynthCommand pOrgan) {
        if (pStorage == null) {
            throw new IllegalArgumentException(ERR_NULL_STORAGE);
        }
        if (pPiano == null || pBass == null || pOrgan == null) {
            throw new IllegalArgumentException(ERR_NULL_COMMAND);
        }
        storage = pStorage;
        piano = pPiano;
        bass = pBass;
        organ = pOrgan;
    }

    /**
     * Reproduce la grabación en el hilo que llama. Al salir (normal o interrumpido)
     * apaga las notas del órgano que quedaron encendidas.
     *
     * @throws InterruptedException si alguien interrumpió el hilo (así se detiene)
     */
    public void play(RecordingHeader pHeader) throws StorageException, InterruptedException {
        if (pHeader == null) {
            throw new IllegalArgumentException(ERR_NULL_HEADER);
        }
        long startMs = System.currentTimeMillis();
        try {
            for (long position = pHeader.getStartPosition(); position < pHeader.getEndPosition();
                    position += RECORD_SIZE) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                SynthEvent event = storage.readEvent(position);
                long waitMs = startMs + event.getTimeMs() - System.currentTimeMillis();
                if (waitMs > PLAYER_NO_WAIT_MS) {
                    Thread.sleep(waitMs);
                }
                send(event);
            }
        } finally {
            releaseHeldOrganNotes();
        }
    }

    /** Reproduce en un hilo propio y vuelve de inmediato. */
    public void start(RecordingHeader pHeader) {
        if (pHeader == null) {
            throw new IllegalArgumentException(ERR_NULL_HEADER);
        }
        synchronized (lifecycleLock) {
            if (playing) {
                throw new IllegalStateException(ERR_ALREADY_PLAYING);
            }
            playing = true;
            thread = new Thread(() -> runPlayback(pHeader), PLAYER_THREAD_NAME);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /** Detiene la reproducción en curso; sin reproducción no hace nada. */
    public void stop() {
        synchronized (lifecycleLock) {
            if (thread == null) {
                return;
            }
            thread.interrupt();
            try {
                thread.join(PLAYER_STOP_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setListener(IPlaybackListener pListener) {
        listener = pListener;
    }

    // ------------------------------------------------------------ auxiliares

    private void runPlayback(RecordingHeader pHeader) {
        try {
            play(pHeader);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // stop() pidió terminar
        } catch (StorageException e) {
            System.err.println(ERR_PLAYER_READ + e.getMessage());
        } finally {
            playing = false;
            IPlaybackListener current = listener;
            if (current != null) {
                current.onPlaybackFinished(pHeader.getName());
            }
        }
    }

    /** Envía el evento al comando de su synth; si falla, lo reporta y sigue. */
    private void send(SynthEvent pEvent) {
        try {
            commandFor(pEvent).execute(pEvent);
            trackOrganNote(pEvent);
        } catch (SynthConnectionException e) {
            System.err.println(ERR_PLAYER_SEND + e.getMessage());
        }
    }

    private ISynthCommand commandFor(SynthEvent pEvent) {
        return switch (pEvent.getSynth()) {
            case PIANO -> piano;
            case BASS -> bass;
            case ORGAN -> organ;
        };
    }

    private void trackOrganNote(SynthEvent pEvent) {
        switch (pEvent.getAction()) {
            case NOTE_ON:
                heldOrganNotes.add(pEvent.getNote());
                break;
            case NOTE_OFF:
                heldOrganNotes.remove(pEvent.getNote());
                break;
            default:
                break; // piano y bajo terminan solos
        }
    }

    private void releaseHeldOrganNotes() {
        List<Integer> notes = new ArrayList<>(heldOrganNotes);
        heldOrganNotes.clear();
        for (int note : notes) {
            try {
                organ.execute(SynthEvent.organOff(note));
            } catch (SynthConnectionException e) {
                System.err.println(ERR_PLAYER_SEND + e.getMessage());
            }
        }
    }
}
