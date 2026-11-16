package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.AUDIO_SAMPLE_RATE;
import static powerband.util.IConstants.NO_PITCH;
import static powerband.util.IConstants.ORGAN_MAX_NOTE;
import static powerband.util.IConstants.ORGAN_MIN_NOTE;

import org.junit.jupiter.api.Test;

import powerband.instrument.helpers.TestSignals;

class PitchDetectorTest {

    private static final float AMPLITUDE = 0.5f;

    private final PitchDetector detector = new PitchDetector(AUDIO_SAMPLE_RATE);

    @Test
    void detectsA440() {
        float frequency = detector.detectFrequency(TestSignals.sine(440f, AMPLITUDE));

        assertTrue(frequency >= 435.6f && frequency <= 444.4f, "frecuencia " + frequency);
        assertEquals(69, detector.toMidiNote(frequency));
    }

    @Test
    void detectsEverySemitoneFromFortyToOneHundred() {
        for (int note = ORGAN_MIN_NOTE; note <= ORGAN_MAX_NOTE; note++) {
            float[] block = TestSignals.sine(TestSignals.noteFrequency(note), AMPLITUDE);

            float frequency = detector.detectFrequency(block);

            assertTrue(frequency != NO_PITCH, "sin tono en la nota " + note);
            assertEquals(note, detector.toMidiNote(frequency), "frecuencia " + frequency);
        }
    }

    @Test
    void detectsFundamentalOfSineWithHarmonics() {
        float frequency = detector.detectFrequency(TestSignals.harmonics(220f, 0.4f));

        assertEquals(57, detector.toMidiNote(frequency), "frecuencia " + frequency);
    }

    @Test
    void detectionDoesNotDependOnVolume() {
        float frequency = detector.detectFrequency(TestSignals.sine(440f, 0.01f));

        assertEquals(69, detector.toMidiNote(frequency), "frecuencia " + frequency);
    }

    @Test
    void returnsNoPitchForSilence() {
        assertEquals(NO_PITCH, detector.detectFrequency(TestSignals.silence()));
    }

    @Test
    void returnsNoPitchForWhiteNoise() {
        for (long seed = 1; seed <= 20; seed++) {
            assertEquals(NO_PITCH, detector.detectFrequency(TestSignals.noise(seed, AMPLITUDE)),
                    "semilla " + seed);
        }
    }

    @Test
    void rmsOfSilenceIsZero() {
        assertEquals(0f, detector.rms(TestSignals.silence()));
    }

    @Test
    void rmsOfSineIsAmplitudeOverSquareRootOfTwo() {
        assertEquals(0.3536f, detector.rms(TestSignals.sine(440f, 0.5f)), 0.01f);
    }

    @Test
    void toMidiNoteRoundsToNearestSemitone() {
        assertEquals(69, detector.toMidiNote(440f));
        assertEquals(69, detector.toMidiNote(452f));
        assertEquals(70, detector.toMidiNote(455f));
        assertEquals(60, detector.toMidiNote(261.63f));
    }
}
