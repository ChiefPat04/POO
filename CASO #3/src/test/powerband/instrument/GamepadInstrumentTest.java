package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.BASS_MAX_NOTE;
import static powerband.util.IConstants.BASS_MIN_NOTE;
import static powerband.util.IConstants.C_MAJOR_SCALE;
import static powerband.util.IConstants.DEFAULT_BASS_AMP;
import static powerband.util.IConstants.GAMEPAD_AXIS_REST;
import static powerband.util.IConstants.GAMEPAD_INITIAL_STEP;
import static powerband.util.IConstants.GAMEPAD_MAX_DUR_MS;
import static powerband.util.IConstants.GAMEPAD_MAX_STEP;
import static powerband.util.IConstants.GAMEPAD_MIN_DUR_MS;
import static powerband.util.IConstants.GAMEPAD_MIN_STEP;
import static powerband.util.IConstants.GAMEPAD_OCTAVE_STEPS;
import static powerband.util.IConstants.GAMEPAD_POLL_INTERVAL_MS;
import static powerband.util.IConstants.GAMEPAD_STICK_PUSH_THRESHOLD;
import static powerband.util.IConstants.OCTAVE_SEMITONES;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.exception.DeviceException;
import powerband.instrument.helpers.FakeGamepadReader;
import powerband.instrument.helpers.RecordingCommand;
import powerband.model.GamepadState;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;

class GamepadInstrumentTest {

    private static final float DELTA = 1e-6f;
    private static final long WAIT_TIMEOUT_MS = 3000L;
    private static final GamepadState IDLE = GamepadState.idle();

    private RecordingCommand command;
    private FakeGamepadReader reader;
    private GamepadInstrument gamepad;

    @BeforeEach
    void setUp() {
        command = new RecordingCommand();
        reader = new FakeGamepadReader();
        gamepad = new GamepadInstrument(command, reader);
    }

    // ------------------------------------------------------------ mapeo

    @Test
    void pressingCrossPlaysRootNote() throws Exception {
        gamepad.process(buttons("x"));

        assertEquals(1, command.getEvents().size());
        SynthEvent event = command.getEvents().get(0);
        assertEquals(36, event.getNote());
        assertEquals(SynthType.BASS, event.getSynth());
        assertEquals(SynthAction.TRIGGER, event.getAction());
        assertEquals(DEFAULT_BASS_AMP, event.getAmp(), DELTA);
    }

    @Test
    void eachButtonPlaysItsScaleDegree() throws Exception {
        tap("x");
        tap("o");
        tap("s");
        tap("t");

        assertEquals(List.of(36, 38, 40, 41), notes());
    }

    @Test
    void heldButtonPlaysOnlyOnce() throws Exception {
        gamepad.process(buttons("x"));
        gamepad.process(buttons("x"));
        gamepad.process(buttons("x"));

        assertEquals(List.of(36), notes());
    }

    @Test
    void releasingAndPressingAgainPlaysAgain() throws Exception {
        tap("x");
        tap("x");

        assertEquals(List.of(36, 36), notes());
    }

    @Test
    void buttonsPressedTogetherPlayOneNoteEach() throws Exception {
        gamepad.process(buttons("xt"));

        assertEquals(List.of(36, 41), notes());
    }

    @Test
    void stickRightRaisesBaseOneStepPerPush() throws Exception {
        pushStick(1f);
        tap("x");

        assertEquals(List.of(38), notes());
    }

    @Test
    void stickHeldDoesNotKeepMovingBase() throws Exception {
        gamepad.process(stick(1f));
        gamepad.process(stick(1f));
        gamepad.process(stick(1f));
        gamepad.process(IDLE);
        tap("x");

        assertEquals(List.of(38), notes());
    }

    @Test
    void stickMustReturnBelowReleaseThresholdBeforeNextPush() throws Exception {
        gamepad.process(stick(0.7f));
        gamepad.process(stick(0.5f)); // no llegó al umbral de soltar
        gamepad.process(stick(0.7f));
        gamepad.process(IDLE);
        tap("x");

        gamepad.process(stick(0.7f));
        gamepad.process(stick(0.1f)); // ahora sí se rearma
        gamepad.process(stick(0.7f));
        gamepad.process(IDLE);
        tap("x");

        assertEquals(List.of(38, 41), notes()); // 1 paso, luego 2 más
    }

    @Test
    void stickBelowPushThresholdDoesNothing() throws Exception {
        gamepad.process(stick(GAMEPAD_STICK_PUSH_THRESHOLD - 0.1f));
        gamepad.process(IDLE);
        tap("x");

        assertEquals(List.of(36), notes());
    }

    @Test
    void stickLeftLowersBase() throws Exception {
        pushStick(-1f);
        tap("x");

        assertEquals(List.of(35), notes());
    }

    @Test
    void r1RaisesBaseOneOctave() throws Exception {
        tap("r");
        tap("x");

        assertEquals(List.of(48), notes());
    }

    @Test
    void l1LowersBaseOneOctave() throws Exception {
        tap("r");
        tap("l");
        tap("x");

        assertEquals(List.of(36), notes());
    }

    @Test
    void heldShoulderChangesOctaveOnlyOnce() throws Exception {
        // base en el mínimo (28): ahí caben dos octavas hacia arriba, así que una
        // segunda subida indebida se notaría
        for (int i = GAMEPAD_INITIAL_STEP; i > GAMEPAD_MIN_STEP; i--) {
            pushStick(-1f);
        }
        gamepad.process(buttons("r"));
        gamepad.process(buttons("r"));
        gamepad.process(buttons("r"));
        gamepad.process(IDLE);
        tap("x");

        assertEquals(List.of(BASS_MIN_NOTE + OCTAVE_SEMITONES), notes());
    }

    @Test
    void baseChangeOutsideRangeIsIgnored() throws Exception {
        tap("l"); // Do1 quedaría bajo el mínimo
        tap("x");
        tap("r");
        tap("r"); // la segunda octava quedaría sobre el máximo
        tap("x");

        assertEquals(List.of(36, 48), notes());
    }

    @Test
    void noteIsClampedToBassRange() throws Exception {
        moveBaseTo(GAMEPAD_MAX_STEP);
        tap("x");
        tap("o");
        tap("s");
        tap("t");

        assertEquals(List.of(55, 55, 55, 55), notes());
    }

    @Test
    void everyReachableNoteIsInCMajorAndInRange() throws Exception {
        for (int i = GAMEPAD_INITIAL_STEP; i > GAMEPAD_MIN_STEP; i--) {
            pushStick(-1f);
        }
        for (int step = GAMEPAD_MIN_STEP; step <= GAMEPAD_MAX_STEP; step++) {
            gamepad.process(buttons("xost"));
            gamepad.process(IDLE);
            pushStick(1f);
        }

        int positions = GAMEPAD_MAX_STEP - GAMEPAD_MIN_STEP + 1;
        assertEquals(positions * 4, notes().size());
        assertEquals(BASS_MIN_NOTE, notes().get(0));
        for (int note : notes()) {
            assertTrue(note >= BASS_MIN_NOTE && note <= BASS_MAX_NOTE, "nota " + note);
            assertTrue(C_MAJOR_SCALE.contains(Math.floorMod(note, OCTAVE_SEMITONES)),
                    "nota " + note);
        }
    }

    @Test
    void triggerMapsDurationBetweenMinAndMax() throws Exception {
        gamepad.process(trigger(0f));
        gamepad.process(IDLE);
        gamepad.process(trigger(1f));
        gamepad.process(IDLE);
        gamepad.process(trigger(0.5f));

        List<SynthEvent> events = command.getEvents();
        assertEquals(GAMEPAD_MIN_DUR_MS, events.get(0).getDurationMs());
        assertEquals(GAMEPAD_MAX_DUR_MS, events.get(1).getDurationMs());
        assertEquals((GAMEPAD_MIN_DUR_MS + GAMEPAD_MAX_DUR_MS) / 2, events.get(2).getDurationMs());
    }

    @Test
    void baseChangesApplyBeforeButtonsInSameRead() throws Exception {
        gamepad.process(buttons("rx"));

        assertEquals(List.of(48), notes());
    }

    @Test
    void disconnectedStateIsIgnored() throws Exception {
        gamepad.process(new GamepadState(true, false, false, false, false, true,
                1f, GAMEPAD_AXIS_REST, false));
        tap("x");

        assertEquals(List.of(36), notes());
    }

    @Test
    void stepToNoteMapsLimits() {
        assertEquals(28, GamepadInstrument.stepToNote(GAMEPAD_MIN_STEP));
        assertEquals(36, GamepadInstrument.stepToNote(GAMEPAD_INITIAL_STEP));
        assertEquals(48, GamepadInstrument.stepToNote(GAMEPAD_OCTAVE_STEPS));
        assertEquals(55, GamepadInstrument.stepToNote(GAMEPAD_MAX_STEP));
    }

    // ------------------------------------------------------------ ciclo de vida

    @Test
    void startThrowsDeviceExceptionWhenReaderCannotOpen() throws Exception {
        reader.setFailOnOpen(true);

        assertThrows(DeviceException.class, () -> gamepad.start());

        Thread.sleep(GAMEPAD_POLL_INTERVAL_MS * 5);
        assertEquals(0, reader.getReadCount());
        gamepad.stop();
    }

    @Test
    void startTwiceThrowsIllegalStateException() throws Exception {
        gamepad.start();
        try {
            assertThrows(IllegalStateException.class, () -> gamepad.start());
        } finally {
            gamepad.stop();
        }
    }

    @Test
    void pollingThreadPlaysNoteWhenButtonIsPressed() throws Exception {
        gamepad.start();
        try {
            reader.setState(buttons("x"));

            awaitEvents(1);
            awaitReads(reader.getReadCount() + 3); // sigue presionado varias lecturas
            assertEquals(List.of(36), notes());
        } finally {
            gamepad.stop();
        }
    }

    @Test
    void threadSurvivesDisconnectionAndPlaysAfterReconnect() throws Exception {
        reader.setState(GamepadState.disconnected());
        gamepad.start();
        try {
            awaitReads(1);
            assertTrue(command.getEvents().isEmpty());

            reader.setState(buttons("x"));

            awaitEvents(1);
            assertEquals(List.of(36), notes());
        } finally {
            gamepad.stop();
        }
    }

    @Test
    void stopClosesReaderAndStopsPolling() throws Exception {
        gamepad.start();
        awaitReads(1);

        gamepad.stop();
        int readsAfterStop = reader.getReadCount();
        Thread.sleep(GAMEPAD_POLL_INTERVAL_MS * 5);

        assertTrue(reader.isClosed());
        assertEquals(readsAfterStop, reader.getReadCount());
        gamepad.stop(); // dos veces no falla
    }

    @Test
    void stopWithoutStartDoesNotFail() {
        gamepad.stop();

        assertEquals(0, reader.getReadCount());
    }

    // ------------------------------------------------------------ auxiliares

    /**
     * Estado conectado con los botones indicados: x = equis, o = círculo,
     * s = cuadrado, t = triángulo, l = L1, r = R1.
     */
    private static GamepadState state(String pPressed, float pStickX, float pTrigger) {
        return new GamepadState(pPressed.contains("x"), pPressed.contains("o"),
                pPressed.contains("s"), pPressed.contains("t"), pPressed.contains("l"),
                pPressed.contains("r"), pStickX, pTrigger, true);
    }

    private static GamepadState buttons(String pPressed) {
        return state(pPressed, GAMEPAD_AXIS_REST, GAMEPAD_AXIS_REST);
    }

    private static GamepadState stick(float pStickX) {
        return state("", pStickX, GAMEPAD_AXIS_REST);
    }

    /** Equis presionado con el gatillo R2 en ese valor. */
    private static GamepadState trigger(float pTrigger) {
        return state("x", GAMEPAD_AXIS_REST, pTrigger);
    }

    private void tap(String pPressed) throws Exception {
        gamepad.process(buttons(pPressed));
        gamepad.process(IDLE);
    }

    private void pushStick(float pStickX) throws Exception {
        gamepad.process(stick(pStickX));
        gamepad.process(IDLE);
    }

    private void moveBaseTo(int pStep) throws Exception {
        for (int i = GAMEPAD_INITIAL_STEP; i < pStep; i++) {
            pushStick(1f);
        }
    }

    private List<Integer> notes() {
        List<Integer> notes = new ArrayList<>();
        for (SynthEvent event : command.getEvents()) {
            notes.add(event.getNote());
        }
        return notes;
    }

    private void awaitEvents(int pCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (command.getEvents().size() < pCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(GAMEPAD_POLL_INTERVAL_MS);
        }
        assertTrue(command.getEvents().size() >= pCount, "no llegaron los eventos a tiempo");
    }

    private void awaitReads(int pCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (reader.getReadCount() < pCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(GAMEPAD_POLL_INTERVAL_MS);
        }
        assertTrue(reader.getReadCount() >= pCount, "el hilo no leyó el control a tiempo");
    }
}
