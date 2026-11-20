package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.CAM_AMP_GAIN;
import static powerband.util.IConstants.CAM_DUR_MS;
import static powerband.util.IConstants.CAM_MAX_AMP;
import static powerband.util.IConstants.CAM_MIN_AMP;
import static powerband.util.IConstants.CAM_REARM_MOTION;
import static powerband.util.IConstants.CAM_STRIKE_MOTION;
import static powerband.util.IConstants.C_MAJOR_SCALE;
import static powerband.util.IConstants.OCTAVE_SEMITONES;
import static powerband.util.IConstants.PIANO_MAX_NOTE;
import static powerband.util.IConstants.PIANO_MIN_NOTE;
import static powerband.util.IConstants.THIRD_MAJOR;
import static powerband.util.IConstants.THIRD_MINOR;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.exception.DeviceException;
import powerband.instrument.helpers.FakeFrameSource;
import powerband.instrument.helpers.RecordingCommand;
import powerband.instrument.helpers.TestFrames;
import powerband.model.HandReading;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;

class WebcamInstrumentTest {

    private static final float DELTA = 0.001f;
    private static final long WAIT_TIMEOUT_MS = 3000L;
    private static final long WAIT_STEP_MS = 5L;
    private static final float CENTER = 0.5f;
    private static final float LEFT = 0.1f;
    private static final float RIGHT = 0.9f;
    /** Movimientos calculados desde los umbrales, para que sigan valiendo si se ajustan. */
    private static final float STRIKE = CAM_STRIKE_MOTION;
    private static final float BETWEEN = (CAM_REARM_MOTION + CAM_STRIKE_MOTION) / 2f;
    private static final float LOW = CAM_REARM_MOTION / 2f;
    private static final float HUGE = CAM_STRIKE_MOTION * 10f;
    private static final float SQUARE = 0.3f;

    private RecordingCommand command;
    private FakeFrameSource source;
    private WebcamInstrument webcam;

    @BeforeEach
    void setUp() {
        command = new RecordingCommand();
        source = new FakeFrameSource();
        webcam = new WebcamInstrument(command, source);
    }

    // ------------------------------------------------------------ reglas

    @Test
    void strikeTriggersOneChord() throws Exception {
        webcam.process(hand(CENTER, CENTER, STRIKE));

        assertEquals(1, command.getEvents().size());
        SynthEvent event = command.getEvents().get(0);
        assertEquals(SynthType.PIANO, event.getSynth());
        assertEquals(SynthAction.TRIGGER, event.getAction());
    }

    @Test
    void sustainedMotionDoesNotRepeat() throws Exception {
        for (int i = 0; i < 10; i++) {
            webcam.process(hand(CENTER, CENTER, STRIKE));
        }

        assertEquals(1, command.getEvents().size());
    }

    @Test
    void lowMotionRearms() throws Exception {
        webcam.process(hand(CENTER, CENTER, STRIKE));
        webcam.process(hand(CENTER, CENTER, LOW));
        webcam.process(hand(CENTER, CENTER, STRIKE));

        assertEquals(2, command.getEvents().size());
    }

    @Test
    void absentHandRearms() throws Exception {
        webcam.process(hand(CENTER, CENTER, STRIKE));
        webcam.process(HandReading.absent());
        webcam.process(hand(CENTER, CENTER, STRIKE));

        assertEquals(2, command.getEvents().size());
    }

    @Test
    void motionBetweenThresholdsDoesNotRearm() throws Exception {
        webcam.process(hand(CENTER, CENTER, STRIKE));
        webcam.process(hand(CENTER, CENTER, BETWEEN));
        webcam.process(hand(CENTER, CENTER, STRIKE));

        assertEquals(1, command.getEvents().size());
    }

    @Test
    void motionBetweenThresholdsDoesNotTrigger() throws Exception {
        for (int i = 0; i < 10; i++) {
            webcam.process(hand(CENTER, CENTER, BETWEEN));
        }

        assertTrue(command.getEvents().isEmpty());
    }

    @Test
    void topOfImageIsHighestNote() throws Exception {
        webcam.process(hand(CENTER, 0f, STRIKE));

        assertEquals(72, command.getEvents().get(0).getNote());
        assertEquals(72, WebcamInstrument.heightToNote(0f));
    }

    @Test
    void bottomOfImageIsLowestNote() throws Exception {
        webcam.process(hand(CENTER, 1f, STRIKE));

        assertEquals(48, command.getEvents().get(0).getNote());
        assertEquals(48, WebcamInstrument.heightToNote(1f));
    }

    @Test
    void heightToNoteCoversScaleInOrder() {
        List<Integer> expected = List.of(48, 50, 52, 53, 55, 57, 59, 60, 62, 64, 65, 67, 69, 71, 72);
        int bands = expected.size();
        List<Integer> notes = new ArrayList<>();
        for (int band = 0; band < bands; band++) {
            float y = 1f - (band + 0.5f) / bands; // centro de cada franja, de abajo hacia arriba
            notes.add(WebcamInstrument.heightToNote(y));
        }

        assertEquals(expected, notes);
    }

    @Test
    void allNotesAreInRangeAndInCMajor() {
        for (float y = -0.5f; y <= 1.5f; y += 0.01f) {
            int note = WebcamInstrument.heightToNote(y);
            assertTrue(note >= PIANO_MIN_NOTE && note <= PIANO_MAX_NOTE, "fuera de rango: " + note);
            assertTrue(C_MAJOR_SCALE.contains(Math.floorMod(note, OCTAVE_SEMITONES)),
                    "fuera de Do mayor: " + note);
        }
    }

    @Test
    void leftSideIsMajorRightSideIsMinor() throws Exception {
        webcam.process(hand(LEFT, CENTER, STRIKE));
        webcam.process(HandReading.absent());
        webcam.process(hand(RIGHT, CENTER, STRIKE));

        assertEquals(THIRD_MAJOR, command.getEvents().get(0).getThird());
        assertEquals(THIRD_MINOR, command.getEvents().get(1).getThird());
    }

    @Test
    void ampFollowsMotionAndIsClamped() throws Exception {
        float intermediate = (CAM_MIN_AMP + CAM_MAX_AMP) / 2f / CAM_AMP_GAIN;
        float[] motions = {STRIKE, Math.max(STRIKE, intermediate), HUGE};
        for (float motion : motions) {
            webcam.process(hand(CENTER, CENTER, motion));
            webcam.process(HandReading.absent());
        }

        for (int i = 0; i < motions.length; i++) {
            float expected = Math.max(CAM_MIN_AMP, Math.min(CAM_MAX_AMP, motions[i] * CAM_AMP_GAIN));
            assertEquals(expected, command.getEvents().get(i).getAmp(), DELTA);
        }
        assertEquals(CAM_MAX_AMP, command.getEvents().get(2).getAmp(), DELTA);
    }

    @Test
    void durationIsFixed() throws Exception {
        webcam.process(hand(CENTER, CENTER, STRIKE));

        assertEquals(CAM_DUR_MS, command.getEvents().get(0).getDurationMs());
    }

    // ------------------------------------------------------------ ciclo de vida

    @Test
    void startThrowsDeviceExceptionWhenSourceCannotOpen() {
        source.setFailOnOpen(true);

        assertThrows(DeviceException.class, webcam::start);
        webcam.stop();
        assertFalse(source.isClosed()); // no arrancó: no hay nada que cerrar
    }

    @Test
    void startTwiceThrowsIllegalStateException() throws Exception {
        try {
            webcam.start();
            assertThrows(IllegalStateException.class, webcam::start);
        } finally {
            webcam.stop();
        }
    }

    @Test
    void threadTriggersChordFromMovingSquare() throws Exception {
        source.setFrames(TestFrames.withSquare(0.2f, CENTER, SQUARE),
                TestFrames.withSquare(0.8f, CENTER, SQUARE));
        try {
            webcam.start();
            awaitEvents(1);
        } finally {
            webcam.stop();
        }

        assertEquals(SynthAction.TRIGGER, command.getEvents().get(0).getAction());
    }

    @Test
    void sourceWithoutFramesKeepsThreadRunning() throws Exception {
        source.setHasFrames(false);
        try {
            webcam.start();
            awaitEmptyReads(2);
            assertTrue(command.getEvents().isEmpty());

            source.setFrames(TestFrames.withSquare(0.2f, CENTER, SQUARE),
                    TestFrames.withSquare(0.8f, CENTER, SQUARE));
            source.setHasFrames(true);
            awaitEvents(1);
        } finally {
            webcam.stop();
        }
    }

    @Test
    void stopClosesSource() throws Exception {
        webcam.start();
        webcam.stop();

        assertTrue(source.isClosed());
    }

    @Test
    void stopWithoutStartDoesNotFail() {
        webcam.stop();
        webcam.stop();

        assertFalse(source.isClosed());
    }

    // ------------------------------------------------------------ auxiliares

    private static HandReading hand(float pX, float pY, float pMotion) {
        return new HandReading(true, pX, pY, pMotion);
    }

    private void awaitEvents(int pCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (command.getEvents().size() < pCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(WAIT_STEP_MS);
        }
        assertTrue(command.getEvents().size() >= pCount, "no llegaron los eventos a tiempo");
    }

    private void awaitEmptyReads(int pCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (source.getEmptyReads() < pCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(WAIT_STEP_MS);
        }
        assertTrue(source.getEmptyReads() >= pCount, "el hilo no siguió leyendo");
    }
}
