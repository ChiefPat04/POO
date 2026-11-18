package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.AUDIO_SAMPLE_RATE;
import static powerband.util.IConstants.MIC_AMP_GAIN;
import static powerband.util.IConstants.MIC_MAX_AMP;
import static powerband.util.IConstants.MIC_MIN_AMP;
import static powerband.util.IConstants.MIC_OFF_THRESHOLD;
import static powerband.util.IConstants.MIC_OFF_WINDOWS;
import static powerband.util.IConstants.MIC_ON_THRESHOLD;
import static powerband.util.IConstants.MIC_STABLE_WINDOWS;
import static powerband.util.IConstants.NO_NOTE;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.exception.DeviceException;
import powerband.instrument.helpers.FakeAudioSource;
import powerband.instrument.helpers.RecordingCommand;
import powerband.instrument.helpers.TestSignals;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;

class MicrophoneInstrumentTest {

    private static final float DELTA = 0.01f;
    private static final long WAIT_TIMEOUT_MS = 3000L;
    private static final long WAIT_STEP_MS = 5L;
    private static final float LOUD = 0.2f;
    private static final float SQRT_2 = (float) Math.sqrt(2.0);
    /** Amplitud de un seno cuyo RMS queda a medio camino entre los dos umbrales. */
    private static final float BETWEEN = (MIC_ON_THRESHOLD + MIC_OFF_THRESHOLD) / 2f * SQRT_2;

    private static final float[] A4 = TestSignals.sine(440f, LOUD);
    private static final float[] C5 = TestSignals.sine(523.25f, LOUD);
    private static final float[] SILENCE = TestSignals.silence();

    private RecordingCommand command;
    private FakeAudioSource source;
    private MicrophoneInstrument microphone;

    @BeforeEach
    void setUp() {
        command = new RecordingCommand();
        source = new FakeAudioSource();
        microphone = new MicrophoneInstrument(command, source);
    }

    // ------------------------------------------------------------ reglas

    @Test
    void stableLoudNoteSendsOrganOn() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS);

        assertEquals(List.of("ON 69"), events());
        assertEquals(SynthType.ORGAN, command.getEvents().get(0).getSynth());
    }

    @Test
    void noteDoesNotPlayBeforeStableWindows() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS - 1);

        assertTrue(command.getEvents().isEmpty());
    }

    @Test
    void heldNoteIsNotRepeated() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS + 10);

        assertEquals(List.of("ON 69"), events());
    }

    @Test
    void ampFollowsVolumeAndIsClamped() throws Exception {
        float softRms = MIC_ON_THRESHOLD * 1.1f;
        float midRms = (MIC_MIN_AMP + MIC_MAX_AMP) / 2f / MIC_AMP_GAIN;
        assertEquals(MIC_MIN_AMP, ampOfNoteStartedWith(softRms * SQRT_2), DELTA);
        assertEquals(MIC_MAX_AMP, ampOfNoteStartedWith(0.9f), DELTA);
        assertEquals(midRms * MIC_AMP_GAIN, ampOfNoteStartedWith(midRms * SQRT_2), DELTA);
    }

    @Test
    void noteChangeSendsOffThenOn() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS);
        feed(C5, MIC_STABLE_WINDOWS);

        assertEquals(List.of("ON 69", "OFF 69", "ON 72"), events());
    }

    @Test
    void briefDifferentNoteDoesNotChangeNote() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS);
        feed(C5, MIC_STABLE_WINDOWS - 1);
        feed(A4, MIC_STABLE_WINDOWS + 2);

        assertEquals(List.of("ON 69"), events());
    }

    @Test
    void sustainedSilenceSendsOrganOff() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS);
        feed(SILENCE, MIC_OFF_WINDOWS);

        assertEquals(List.of("ON 69", "OFF 69"), events());
    }

    @Test
    void shortSilenceKeepsNoteOn() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS);
        feed(SILENCE, MIC_OFF_WINDOWS - 1);
        feed(A4, MIC_STABLE_WINDOWS + 2);
        feed(SILENCE, MIC_OFF_WINDOWS - 1); // la cuenta de silencio empezó de nuevo

        assertEquals(List.of("ON 69"), events());
    }

    @Test
    void volumeBetweenThresholdsKeepsNoteOn() throws Exception {
        feed(A4, MIC_STABLE_WINDOWS);
        feed(TestSignals.sine(440f, BETWEEN), 20);
        feed(TestSignals.sine(523.25f, BETWEEN), 20); // tampoco cambia de nota

        assertEquals(List.of("ON 69"), events());
    }

    @Test
    void toneBetweenThresholdsDoesNotStartNote() throws Exception {
        feed(TestSignals.sine(440f, BETWEEN), 20);

        assertTrue(command.getEvents().isEmpty());
    }

    @Test
    void silenceNeverPlays() throws Exception {
        feed(SILENCE, 20);

        assertTrue(command.getEvents().isEmpty());
    }

    @Test
    void noiseNeverPlays() throws Exception {
        for (long seed = 1; seed <= 20; seed++) {
            microphone.process(TestSignals.noise(seed, 0.5f));
        }

        assertTrue(command.getEvents().isEmpty());
    }

    @Test
    void outOfRangeNotesAreNotPlayed() throws Exception {
        feed(TestSignals.sine(77.78f, LOUD), 10); // nota 39
        feed(TestSignals.sine(2793.83f, LOUD), 10); // nota 101

        assertTrue(command.getEvents().isEmpty());
    }

    @Test
    void neverMoreThanOneNoteOn() throws Exception {
        feed(A4, 5);
        feed(C5, 5);
        feed(SILENCE, 5);
        feed(TestSignals.sine(659.26f, LOUD), 5);
        feed(A4, 1);
        feed(SILENCE, 1);
        feed(TestSignals.sine(329.63f, LOUD), 5);
        feed(TestSignals.noise(7L, 0.5f), 3);
        feed(C5, 5);
        feed(SILENCE, 5);

        int active = NO_NOTE;
        for (SynthEvent event : command.getEvents()) {
            if (event.getAction() == SynthAction.NOTE_ON) {
                assertEquals(NO_NOTE, active, "se encendió una nota con otra sonando");
                active = event.getNote();
            } else {
                assertEquals(active, event.getNote(), "se apagó una nota que no sonaba");
                active = NO_NOTE;
            }
        }
        assertEquals(NO_NOTE, active);
        assertFalse(command.getEvents().isEmpty());
    }

    // ------------------------------------------------------------ ciclo de vida

    @Test
    void startThrowsDeviceExceptionWhenSourceCannotOpen() {
        source.setFailOnOpen(true);

        assertThrows(DeviceException.class, () -> microphone.start());

        microphone.stop();
        assertFalse(source.isClosed()); // nunca arrancó: no hay nada que cerrar
    }

    @Test
    void startTwiceThrowsIllegalStateException() throws Exception {
        microphone.start();
        try {
            assertThrows(IllegalStateException.class, () -> microphone.start());
        } finally {
            microphone.stop();
        }
    }

    @Test
    void threadPlaysNoteFromSource() throws Exception {
        source.setBlock(A4);
        microphone.start();
        try {
            awaitEvents(1);

            assertEquals("ON 69", events().get(0));
        } finally {
            microphone.stop();
        }
    }

    @Test
    void stopReleasesActiveNoteAndClosesSource() throws Exception {
        source.setBlock(A4);
        microphone.start();
        try {
            awaitEvents(1);
        } finally {
            microphone.stop();
        }

        assertEquals(List.of("ON 69", "OFF 69"), events());
        assertTrue(source.isClosed());
        microphone.stop(); // dos veces no falla
        assertEquals(2, command.getEvents().size());
    }

    @Test
    void sourceWithoutAudioReleasesNoteAndThreadKeepsRunning() throws Exception {
        source.setBlock(A4);
        microphone.start();
        try {
            awaitEvents(1);

            source.setHasAudio(false);
            awaitEvents(2);

            source.setHasAudio(true);
            awaitEvents(3);

            assertEquals(List.of("ON 69", "OFF 69", "ON 69"), events().subList(0, 3));
        } finally {
            microphone.stop();
        }
    }

    @Test
    void stopWithoutStartDoesNotFail() {
        microphone.stop();

        assertTrue(command.getEvents().isEmpty());
        assertFalse(source.isClosed());
    }

    // ------------------------------------------------------------ auxiliares

    private void feed(float[] pBlock, int pTimes) throws Exception {
        for (int i = 0; i < pTimes; i++) {
            microphone.process(pBlock);
        }
    }

    /** Enciende La4 con un seno de esa amplitud en un instrumento nuevo y devuelve el amp enviado. */
    private float ampOfNoteStartedWith(float pAmplitude) throws Exception {
        RecordingCommand recorded = new RecordingCommand();
        MicrophoneInstrument instrument = new MicrophoneInstrument(recorded, new FakeAudioSource());
        float[] block = TestSignals.sine(440f, pAmplitude);
        for (int i = 0; i < MIC_STABLE_WINDOWS; i++) {
            instrument.process(block);
        }
        assertEquals(1, recorded.getEvents().size(), "amplitud " + pAmplitude);
        return recorded.getEvents().get(0).getAmp();
    }

    /** Eventos como texto: "ON 69", "OFF 69". */
    private List<String> events() {
        List<String> events = new ArrayList<>();
        for (SynthEvent event : command.getEvents()) {
            String action = event.getAction() == SynthAction.NOTE_ON ? "ON " : "OFF ";
            events.add(action + event.getNote());
        }
        return events;
    }

    private void awaitEvents(int pCount) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (command.getEvents().size() < pCount && System.currentTimeMillis() < deadline) {
            Thread.sleep(WAIT_STEP_MS);
        }
        assertTrue(command.getEvents().size() >= pCount, "no llegaron los eventos a tiempo");
    }
}
