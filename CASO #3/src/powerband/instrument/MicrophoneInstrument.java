package powerband.instrument;

import static powerband.util.IConstants.AUDIO_BLOCK_SIZE;
import static powerband.util.IConstants.AUDIO_SAMPLE_RATE;
import static powerband.util.IConstants.ERR_MIC_ALREADY_STARTED;
import static powerband.util.IConstants.ERR_MIC_PLAY;
import static powerband.util.IConstants.ERR_NULL_AUDIO_SOURCE;
import static powerband.util.IConstants.MIC_AMP_GAIN;
import static powerband.util.IConstants.MIC_MAX_AMP;
import static powerband.util.IConstants.MIC_MIN_AMP;
import static powerband.util.IConstants.MIC_NO_WINDOWS;
import static powerband.util.IConstants.MIC_OFF_THRESHOLD;
import static powerband.util.IConstants.MIC_OFF_WINDOWS;
import static powerband.util.IConstants.MIC_ON_THRESHOLD;
import static powerband.util.IConstants.MIC_RETRY_WAIT_MS;
import static powerband.util.IConstants.MIC_STABLE_WINDOWS;
import static powerband.util.IConstants.MIC_STOP_TIMEOUT_MS;
import static powerband.util.IConstants.MIC_THREAD_NAME;
import static powerband.util.IConstants.NO_NOTE;
import static powerband.util.IConstants.NO_PITCH;
import static powerband.util.IConstants.ORGAN_MAX_NOTE;
import static powerband.util.IConstants.ORGAN_MIN_NOTE;

import powerband.command.ISynthCommand;
import powerband.exception.DeviceException;
import powerband.exception.SynthConnectionException;
import powerband.model.SynthEvent;

/**
 * Micrófono que toca el órgano. Un hilo propio lee ventanas de audio y
 * {@link #process} decide, con el volumen y la nota de cada una:
 * <ul>
 *   <li>volumen sobre el umbral de encendido y la misma nota en varias ventanas
 *       seguidas: {@code organOn};</li>
 *   <li>la nota estable cambia: {@code organOff} de la anterior y {@code organOn} de la nueva;</li>
 *   <li>volumen bajo el umbral de apagado durante varias ventanas: {@code organOff}.</li>
 * </ul>
 * Solo hay una nota encendida a la vez.
 */
public class MicrophoneInstrument extends Instrument {

    private final IAudioSource source;
    private final PitchDetector detector = new PitchDetector(AUDIO_SAMPLE_RATE);
    private final Object lifecycleLock = new Object(); // start/stop; aparte del lock de process

    private int activeNote = NO_NOTE;
    private int candidateNote = NO_NOTE;
    private int candidateCount = MIC_NO_WINDOWS;
    private int quietCount = MIC_NO_WINDOWS;

    private volatile boolean running;
    private Thread thread;

    public MicrophoneInstrument(ISynthCommand pCommand, IAudioSource pSource) {
        super(pCommand);
        if (pSource == null) {
            throw new IllegalArgumentException(ERR_NULL_AUDIO_SOURCE);
        }
        source = pSource;
    }

    /**
     * Abre el micrófono y arranca el hilo de lectura.
     *
     * @throws DeviceException si no hay micrófono o no se puede abrir
     */
    @Override
    public void start() throws DeviceException {
        synchronized (lifecycleLock) {
            if (thread != null) {
                throw new IllegalStateException(ERR_MIC_ALREADY_STARTED);
            }
            source.open();
            running = true;
            thread = new Thread(this::readLoop, MIC_THREAD_NAME);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /** Detiene el hilo, apaga la nota encendida y cierra el micrófono. */
    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            if (thread == null) {
                return;
            }
            running = false;
            thread.interrupt();
            try {
                thread.join(MIC_STOP_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
            try {
                releaseActiveNote();
            } catch (SynthConnectionException e) {
                System.err.println(ERR_MIC_PLAY + e.getMessage());
            }
            source.close();
        }
    }

    /** Analiza una ventana de audio y enciende, cambia o apaga la nota. */
    public synchronized void process(float[] pBlock) throws SynthConnectionException {
        float volume = detector.rms(pBlock);
        if (volume < MIC_OFF_THRESHOLD) {
            clearCandidate();
            quietCount++;
            if (quietCount >= MIC_OFF_WINDOWS) {
                releaseActiveNote();
            }
            return;
        }
        quietCount = MIC_NO_WINDOWS;
        if (volume < MIC_ON_THRESHOLD) {
            clearCandidate(); // zona de histéresis: se sostiene lo que suena
            return;
        }

        int note = detectNote(pBlock);
        if (note == NO_NOTE) {
            clearCandidate();
            return;
        }
        if (note != candidateNote) {
            candidateNote = note;
            candidateCount = MIC_NO_WINDOWS;
        }
        candidateCount++;
        if (candidateCount >= MIC_STABLE_WINDOWS && note != activeNote) {
            releaseActiveNote();
            play(SynthEvent.organOn(note, ampFor(volume)));
            activeNote = note;
        }
    }

    // ------------------------------------------------------------ auxiliares

    /** Nota MIDI de la ventana, o {@code NO_NOTE} si no hay tono o sale del rango del órgano. */
    private int detectNote(float[] pBlock) {
        float frequency = detector.detectFrequency(pBlock);
        if (frequency == NO_PITCH) {
            return NO_NOTE;
        }
        int note = detector.toMidiNote(frequency);
        return note >= ORGAN_MIN_NOTE && note <= ORGAN_MAX_NOTE ? note : NO_NOTE;
    }

    private static float ampFor(float pVolume) {
        return Math.max(MIC_MIN_AMP, Math.min(MIC_MAX_AMP, pVolume * MIC_AMP_GAIN));
    }

    private void clearCandidate() {
        candidateNote = NO_NOTE;
        candidateCount = MIC_NO_WINDOWS;
    }

    private synchronized void releaseActiveNote() throws SynthConnectionException {
        if (activeNote != NO_NOTE) {
            int note = activeNote;
            activeNote = NO_NOTE;
            play(SynthEvent.organOff(note));
        }
    }

    /** Ciclo del hilo. Si la fuente deja de dar audio, apaga la nota, espera y reintenta. */
    private void readLoop() {
        float[] block = new float[AUDIO_BLOCK_SIZE];
        while (running) {
            boolean hasAudio = source.read(block);
            try {
                if (hasAudio) {
                    process(block);
                } else {
                    releaseActiveNote();
                }
            } catch (SynthConnectionException e) {
                System.err.println(ERR_MIC_PLAY + e.getMessage());
            }
            if (!hasAudio) {
                try {
                    Thread.sleep(MIC_RETRY_WAIT_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // stop() pidió terminar
                    return;
                }
            }
        }
    }
}
