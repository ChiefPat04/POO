package powerband.instrument;

import static powerband.util.IConstants.BASS_MAX_NOTE;
import static powerband.util.IConstants.BASS_MIN_NOTE;
import static powerband.util.IConstants.C_MAJOR_SCALE;
import static powerband.util.IConstants.ERR_GAMEPAD_ALREADY_STARTED;
import static powerband.util.IConstants.ERR_GAMEPAD_PLAY;
import static powerband.util.IConstants.ERR_NULL_READER;
import static powerband.util.IConstants.GAMEPAD_INITIAL_STEP;
import static powerband.util.IConstants.GAMEPAD_MAX_DUR_MS;
import static powerband.util.IConstants.GAMEPAD_MAX_STEP;
import static powerband.util.IConstants.GAMEPAD_MIN_DUR_MS;
import static powerband.util.IConstants.GAMEPAD_MIN_STEP;
import static powerband.util.IConstants.GAMEPAD_OCTAVE_STEPS;
import static powerband.util.IConstants.GAMEPAD_POLL_INTERVAL_MS;
import static powerband.util.IConstants.GAMEPAD_RECONNECT_WAIT_MS;
import static powerband.util.IConstants.GAMEPAD_ROOT_NOTE;
import static powerband.util.IConstants.GAMEPAD_STEP_CIRCLE;
import static powerband.util.IConstants.GAMEPAD_STEP_CROSS;
import static powerband.util.IConstants.GAMEPAD_STEP_SQUARE;
import static powerband.util.IConstants.GAMEPAD_STEP_TRIANGLE;
import static powerband.util.IConstants.GAMEPAD_STICK_PUSH_THRESHOLD;
import static powerband.util.IConstants.GAMEPAD_STICK_RELEASE_THRESHOLD;
import static powerband.util.IConstants.GAMEPAD_STICK_STEP;
import static powerband.util.IConstants.GAMEPAD_STOP_TIMEOUT_MS;
import static powerband.util.IConstants.GAMEPAD_THREAD_NAME;
import static powerband.util.IConstants.OCTAVE_SEMITONES;

import powerband.command.ISynthCommand;
import powerband.exception.DeviceException;
import powerband.exception.SynthConnectionException;
import powerband.model.GamepadState;
import powerband.model.SynthEvent;

/**
 * Control de PS5 que toca el bajo. Un hilo propio lee el estado del control y
 * {@link #process} lo traduce en notas:
 * <ul>
 *   <li>equis, círculo, cuadrado y triángulo: 4 grados seguidos de Do mayor desde la base;
 *       una nota por pulsación (flanco de subida);</li>
 *   <li>stick izquierdo horizontal: mueve la base un paso de la escala por empujón;</li>
 *   <li>L1 / R1: bajan o suben la base una octava;</li>
 *   <li>R2: duración de la nota.</li>
 * </ul>
 * La base es un solo entero: pasos de la escala contados desde {@code GAMEPAD_ROOT_NOTE}.
 */
public class GamepadInstrument extends Instrument {

    private final IGamepadReader reader;
    private final Object lifecycleLock = new Object(); // start/stop; aparte del lock de process

    private int baseStep = GAMEPAD_INITIAL_STEP;
    private boolean stickArmed = true;
    private GamepadState previous = GamepadState.idle();

    private volatile boolean running;
    private Thread thread;

    public GamepadInstrument(ISynthCommand pCommand, IGamepadReader pReader) {
        super(pCommand);
        if (pReader == null) {
            throw new IllegalArgumentException(ERR_NULL_READER);
        }
        reader = pReader;
    }

    /** Nota MIDI de un paso de la escala de Do mayor, limitada al rango del bajo. */
    public static int stepToNote(int pStep) {
        int degrees = C_MAJOR_SCALE.size();
        int note = GAMEPAD_ROOT_NOTE
                + OCTAVE_SEMITONES * Math.floorDiv(pStep, degrees)
                + C_MAJOR_SCALE.get(Math.floorMod(pStep, degrees));
        return Math.max(BASS_MIN_NOTE, Math.min(BASS_MAX_NOTE, note));
    }

    /**
     * Inicializa el control y arranca el hilo de lectura.
     *
     * @throws DeviceException si no hay control conectado o la librería no inicializa
     */
    @Override
    public void start() throws DeviceException {
        synchronized (lifecycleLock) {
            if (thread != null) {
                throw new IllegalStateException(ERR_GAMEPAD_ALREADY_STARTED);
            }
            reader.open();
            running = true;
            thread = new Thread(this::pollLoop, GAMEPAD_THREAD_NAME);
            thread.setDaemon(true);
            thread.start();
        }
    }

    /** Detiene el hilo y libera el control. No falla si no estaba iniciado. */
    @Override
    public void stop() {
        synchronized (lifecycleLock) {
            if (thread == null) {
                return;
            }
            running = false;
            thread.interrupt();
            try {
                thread.join(GAMEPAD_STOP_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
            reader.close();
        }
    }

    /**
     * Traduce una lectura del control. Primero mueve la base (stick, L1, R1) y
     * después dispara los botones que pasaron de sueltos a presionados.
     */
    public synchronized void process(GamepadState pState) throws SynthConnectionException {
        if (!pState.isConnected()) {
            return;
        }
        moveBaseWithStick(pState.getLeftStickX());
        if (pState.isR1() && !previous.isR1()) {
            moveBase(GAMEPAD_OCTAVE_STEPS);
        }
        if (pState.isL1() && !previous.isL1()) {
            moveBase(-GAMEPAD_OCTAVE_STEPS);
        }

        // Se guarda antes de tocar: si un envío falla, la pulsación no se repite.
        GamepadState before = previous;
        previous = pState;
        int durationMs = durationFor(pState.getRightTrigger());
        if (pState.isCross() && !before.isCross()) {
            playStep(GAMEPAD_STEP_CROSS, durationMs);
        }
        if (pState.isCircle() && !before.isCircle()) {
            playStep(GAMEPAD_STEP_CIRCLE, durationMs);
        }
        if (pState.isSquare() && !before.isSquare()) {
            playStep(GAMEPAD_STEP_SQUARE, durationMs);
        }
        if (pState.isTriangle() && !before.isTriangle()) {
            playStep(GAMEPAD_STEP_TRIANGLE, durationMs);
        }
    }

    // ------------------------------------------------------------ auxiliares

    /** Un paso por empujón: el stick tiene que volver al centro para dar otro. */
    private void moveBaseWithStick(float pStickX) {
        float magnitude = Math.abs(pStickX);
        if (stickArmed && magnitude >= GAMEPAD_STICK_PUSH_THRESHOLD) {
            stickArmed = false;
            moveBase(Math.round(Math.signum(pStickX)) * GAMEPAD_STICK_STEP);
        } else if (!stickArmed && magnitude <= GAMEPAD_STICK_RELEASE_THRESHOLD) {
            stickArmed = true;
        }
    }

    /** Mueve la base; si el resultado sale del rango, la base no cambia. */
    private void moveBase(int pSteps) {
        int target = baseStep + pSteps;
        if (target >= GAMEPAD_MIN_STEP && target <= GAMEPAD_MAX_STEP) {
            baseStep = target;
        }
    }

    private static int durationFor(float pTrigger) {
        int durationMs = Math.round(
                GAMEPAD_MIN_DUR_MS + pTrigger * (GAMEPAD_MAX_DUR_MS - GAMEPAD_MIN_DUR_MS));
        return Math.max(GAMEPAD_MIN_DUR_MS, Math.min(GAMEPAD_MAX_DUR_MS, durationMs));
    }

    private void playStep(int pButtonStep, int pDurationMs) throws SynthConnectionException {
        play(SynthEvent.bass(stepToNote(baseStep + pButtonStep), pDurationMs));
    }

    /** Ciclo del hilo. Si el control se desconecta, espera y sigue cuando vuelve. */
    private void pollLoop() {
        while (running) {
            GamepadState state = reader.read();
            long waitMs = GAMEPAD_RECONNECT_WAIT_MS;
            if (state.isConnected()) {
                waitMs = GAMEPAD_POLL_INTERVAL_MS;
                try {
                    process(state);
                } catch (SynthConnectionException e) {
                    System.err.println(ERR_GAMEPAD_PLAY + e.getMessage());
                }
            }
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // stop() pidió terminar
                return;
            }
        }
    }
}
