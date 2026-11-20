package powerband.instrument;

import static powerband.util.IConstants.CAM_AMP_GAIN;
import static powerband.util.IConstants.CAM_DUR_MS;
import static powerband.util.IConstants.CAM_GRID_HEIGHT;
import static powerband.util.IConstants.CAM_GRID_WIDTH;
import static powerband.util.IConstants.CAM_MAX_AMP;
import static powerband.util.IConstants.CAM_MAX_STEP;
import static powerband.util.IConstants.CAM_MIN_AMP;
import static powerband.util.IConstants.CAM_NORMALIZED_MAX;
import static powerband.util.IConstants.CAM_NORMALIZED_MIN;
import static powerband.util.IConstants.CAM_NOTE_BANDS;
import static powerband.util.IConstants.CAM_POLL_INTERVAL_MS;
import static powerband.util.IConstants.CAM_REARM_MOTION;
import static powerband.util.IConstants.CAM_RETRY_WAIT_MS;
import static powerband.util.IConstants.CAM_SPLIT_X;
import static powerband.util.IConstants.CAM_STOP_TIMEOUT_MS;
import static powerband.util.IConstants.CAM_STRIKE_MOTION;
import static powerband.util.IConstants.CAM_THREAD_NAME;
import static powerband.util.IConstants.C_MAJOR_SCALE;
import static powerband.util.IConstants.ERR_NULL_FRAME_SOURCE;
import static powerband.util.IConstants.ERR_WEBCAM_ALREADY_STARTED;
import static powerband.util.IConstants.ERR_WEBCAM_PLAY;
import static powerband.util.IConstants.OCTAVE_SEMITONES;
import static powerband.util.IConstants.PIANO_MAX_NOTE;
import static powerband.util.IConstants.PIANO_MIN_NOTE;
import static powerband.util.IConstants.PIANO_ROOT_NOTE;
import static powerband.util.IConstants.THIRD_MAJOR;
import static powerband.util.IConstants.THIRD_MINOR;

import java.awt.image.BufferedImage;

import powerband.command.ISynthCommand;
import powerband.exception.DeviceException;
import powerband.exception.SynthConnectionException;
import powerband.model.HandReading;
import powerband.model.SynthEvent;

/**
 * Webcam que toca el piano. Un hilo propio lee cuadros, {@link HandTracker} dice
 * dónde se movió la mano y {@link #process} decide:
 * <ul>
 *   <li>altura de la mano = nota (arriba más agudo), en Do mayor entre 48 y 72;</li>
 *   <li>lado de la imagen = acorde mayor (izquierda) o menor (derecha);</li>
 *   <li>movimiento brusco = disparo. No vuelve a disparar hasta que el movimiento
 *       baja (histéresis con dos umbrales).</li>
 * </ul>
 */
public class WebcamInstrument extends Instrument {

    private final IFrameSource source;
    private final HandTracker tracker = new HandTracker(CAM_GRID_WIDTH, CAM_GRID_HEIGHT);
    private final Object lifecycleLock = new Object(); // start/stop; aparte del lock de process

    private boolean armed = true;

    private volatile boolean running;
    private Thread thread;

    public WebcamInstrument(ISynthCommand pCommand, IFrameSource pSource) {
        super(pCommand);
        if (pSource == null) {
            throw new IllegalArgumentException(ERR_NULL_FRAME_SOURCE);
        }
        source = pSource;
    }

    /**
     * Abre la cámara y arranca el hilo de lectura.
     *
     * @throws DeviceException si no hay webcam o no se puede abrir
     */
    @Override
    public void start() throws DeviceException {
        synchronized (lifecycleLock) {
            if (thread != null) {
                throw new IllegalStateException(ERR_WEBCAM_ALREADY_STARTED);
            }
            source.open();
            running = true;
            thread = new Thread(this::readLoop, CAM_THREAD_NAME);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /** Detiene el hilo y cierra la cámara. El piano termina solo: no hay nota que apagar. */
    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            if (thread == null) {
                return;
            }
            running = false;
            thread.interrupt();
            try {
                thread.join(CAM_STOP_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
            source.close();
        }
    }

    /** Aplica las reglas a lo que se vio en un cuadro y dispara el acorde si toca. */
    public synchronized void process(HandReading pReading) throws SynthConnectionException {
        float motion = pReading.getMotion();
        if (!pReading.isPresent() || motion < CAM_REARM_MOTION) {
            armed = true;
            return;
        }
        if (motion >= CAM_STRIKE_MOTION && armed) {
            armed = false;
            play(SynthEvent.piano(heightToNote(pReading.getY()), CAM_DUR_MS,
                    thirdFor(pReading.getX()), ampFor(motion)));
        }
        // entre los dos umbrales: ni dispara ni se rearma
    }

    /**
     * Altura normalizada (0 = arriba) → nota de Do mayor entre 48 y 72, en franjas
     * de igual alto.
     */
    public static int heightToNote(float pY) {
        float y = Math.max(CAM_NORMALIZED_MIN, Math.min(CAM_NORMALIZED_MAX, pY));
        int step = Math.min(CAM_MAX_STEP, (int) ((CAM_NORMALIZED_MAX - y) * CAM_NOTE_BANDS));
        int degrees = C_MAJOR_SCALE.size();
        int note = PIANO_ROOT_NOTE
                + OCTAVE_SEMITONES * Math.floorDiv(step, degrees)
                + C_MAJOR_SCALE.get(Math.floorMod(step, degrees));
        return Math.max(PIANO_MIN_NOTE, Math.min(PIANO_MAX_NOTE, note));
    }

    // ------------------------------------------------------------ auxiliares

    private static int thirdFor(float pX) {
        return pX < CAM_SPLIT_X ? THIRD_MAJOR : THIRD_MINOR;
    }

    private static float ampFor(float pMotion) {
        return Math.max(CAM_MIN_AMP, Math.min(CAM_MAX_AMP, pMotion * CAM_AMP_GAIN));
    }

    /** Ciclo del hilo. Si la cámara deja de dar cuadros, espera y reintenta. */
    private void readLoop() {
        while (running) {
            BufferedImage frame = source.read();
            long wait = frame == null ? CAM_RETRY_WAIT_MS : CAM_POLL_INTERVAL_MS;
            try {
                if (frame == null) {
                    tracker.reset();
                    process(HandReading.absent());
                } else {
                    process(tracker.track(frame));
                }
            } catch (SynthConnectionException e) {
                System.err.println(ERR_WEBCAM_PLAY + e.getMessage());
            }
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // stop() pidió terminar
                return;
            }
        }
    }
}
