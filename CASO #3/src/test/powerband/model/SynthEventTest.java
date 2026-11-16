package powerband.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static powerband.util.IConstants.DEFAULT_BASS_AMP;
import static powerband.util.IConstants.DEFAULT_ORGAN_AMP;
import static powerband.util.IConstants.DEFAULT_ORGAN_AMPS;
import static powerband.util.IConstants.DEFAULT_ORGAN_RATIOS;
import static powerband.util.IConstants.DEFAULT_PIANO_AMP;
import static powerband.util.IConstants.THIRD_MINOR;

import org.junit.jupiter.api.Test;

class SynthEventTest {

    private static final float DELTA = 1e-6f;

    @Test
    void pianoFactoryUsesDefaultAmpWhenNotGiven() {
        SynthEvent event = SynthEvent.piano(48, 1500, THIRD_MINOR);

        assertAll(
                () -> assertEquals(SynthType.PIANO, event.getSynth()),
                () -> assertEquals(SynthAction.TRIGGER, event.getAction()),
                () -> assertEquals(48, event.getNote()),
                () -> assertEquals(1500, event.getDurationMs()),
                () -> assertEquals(THIRD_MINOR, event.getThird()),
                () -> assertEquals(DEFAULT_PIANO_AMP, event.getAmp(), DELTA));
    }

    @Test
    void bassFactoryUsesDefaultAmpWhenNotGiven() {
        SynthEvent event = SynthEvent.bass(36, 450);

        assertAll(
                () -> assertEquals(SynthType.BASS, event.getSynth()),
                () -> assertEquals(SynthAction.TRIGGER, event.getAction()),
                () -> assertEquals(DEFAULT_BASS_AMP, event.getAmp(), DELTA));
    }

    @Test
    void organOnFactoryUsesDefaultRatiosAndAmps() {
        SynthEvent event = SynthEvent.organOn(60);

        assertAll(
                () -> assertEquals(SynthType.ORGAN, event.getSynth()),
                () -> assertEquals(SynthAction.NOTE_ON, event.getAction()),
                () -> assertEquals(DEFAULT_ORGAN_AMP, event.getAmp(), DELTA),
                () -> assertEquals(DEFAULT_ORGAN_RATIOS.get(0), event.getR1(), DELTA),
                () -> assertEquals(DEFAULT_ORGAN_RATIOS.get(1), event.getR2(), DELTA),
                () -> assertEquals(DEFAULT_ORGAN_RATIOS.get(2), event.getR3(), DELTA),
                () -> assertEquals(DEFAULT_ORGAN_AMPS.get(0), event.getA1(), DELTA),
                () -> assertEquals(DEFAULT_ORGAN_AMPS.get(1), event.getA2(), DELTA),
                () -> assertEquals(DEFAULT_ORGAN_AMPS.get(2), event.getA3(), DELTA));
    }

    @Test
    void organOffFactoryCreatesNoteOffEvent() {
        SynthEvent event = SynthEvent.organOff(64);

        assertEquals(SynthType.ORGAN, event.getSynth());
        assertEquals(SynthAction.NOTE_OFF, event.getAction());
        assertEquals(64, event.getNote());
    }

    @Test
    void organOnRejectsRatiosOrAmpsWithWrongLength() {
        float[] three = { 1f, 2f, 3f };
        float[] two = { 1f, 2f };

        assertThrows(IllegalArgumentException.class, () -> SynthEvent.organOn(60, 0.3f, two, three));
        assertThrows(IllegalArgumentException.class, () -> SynthEvent.organOn(60, 0.3f, three, two));
        assertThrows(IllegalArgumentException.class, () -> SynthEvent.organOn(60, 0.3f, null, three));
    }

    @Test
    void withTimeMsReturnsNewEventAndKeepsOriginalUnchanged() {
        SynthEvent original = SynthEvent.organOn(64, 0.5f, new float[] { 1f, 2.01f, 4f },
                new float[] { 1f, 0.6f, 0.3f });

        SynthEvent stamped = original.withTimeMs(1234L);

        assertNotSame(original, stamped);
        assertEquals(0L, original.getTimeMs());
        assertEquals(1234L, stamped.getTimeMs());
        assertAll(
                () -> assertEquals(original.getNote(), stamped.getNote()),
                () -> assertEquals(original.getAmp(), stamped.getAmp(), DELTA),
                () -> assertEquals(original.getR2(), stamped.getR2(), DELTA),
                () -> assertEquals(original.getA3(), stamped.getA3(), DELTA));
    }
}
