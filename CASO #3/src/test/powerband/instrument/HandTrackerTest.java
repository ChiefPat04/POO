package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.CAM_GRID_HEIGHT;
import static powerband.util.IConstants.CAM_GRID_WIDTH;
import static powerband.util.IConstants.CAM_MIRROR;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import powerband.instrument.helpers.TestFrames;
import powerband.model.HandReading;

class HandTrackerTest {

    private static final float DELTA = 0.05f;
    private static final float SIZE = 0.2f;

    private HandTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new HandTracker(CAM_GRID_WIDTH, CAM_GRID_HEIGHT);
    }

    @Test
    void firstFrameIsAbsent() {
        assertFalse(tracker.track(TestFrames.withSquare(0.5f, 0.5f, SIZE)).isPresent());
    }

    @Test
    void identicalFramesAreAbsent() {
        BufferedImage frame = TestFrames.withSquare(0.5f, 0.5f, SIZE);
        tracker.track(frame);

        assertFalse(tracker.track(frame).isPresent());
    }

    @Test
    void tinyChangeIsAbsent() {
        tracker.track(TestFrames.blank());

        assertFalse(tracker.track(TestFrames.withPixel(160, 120)).isPresent());
    }

    @Test
    void wholeFrameChangeIsAbsent() {
        tracker.track(TestFrames.blank());

        assertFalse(tracker.track(TestFrames.filled()).isPresent());
    }

    @Test
    void movingSquareIsPresentAtItsCenter() {
        HandReading reading = move(0.2f, 0.5f, 0.3f, 0.5f);

        assertTrue(reading.isPresent());
        float expectedX = CAM_MIRROR ? 1f - 0.25f : 0.25f;
        assertEquals(expectedX, reading.getX(), DELTA);
        assertEquals(0.5f, reading.getY(), DELTA);
    }

    @Test
    void higherSquareGivesSmallerY() {
        HandReading high = move(0.2f, 0.25f, 0.3f, 0.25f);
        tracker.reset();
        HandReading low = move(0.2f, 0.75f, 0.3f, 0.75f);

        assertTrue(high.isPresent());
        assertTrue(low.isPresent());
        assertTrue(high.getY() < low.getY());
    }

    @Test
    void biggerMovementGivesMoreMotion() {
        HandReading small = move(0.4f, 0.5f, 0.45f, 0.5f);
        tracker.reset();
        HandReading big = move(0.2f, 0.5f, 0.6f, 0.5f);

        assertTrue(small.isPresent());
        assertTrue(big.isPresent());
        assertTrue(big.getMotion() > small.getMotion());
    }

    @Test
    void resetForgetsPreviousFrame() {
        tracker.track(TestFrames.withSquare(0.2f, 0.5f, SIZE));
        tracker.reset();

        assertFalse(tracker.track(TestFrames.withSquare(0.6f, 0.5f, SIZE)).isPresent());
    }

    // ------------------------------------------------------------ auxiliares

    private HandReading move(float pFromX, float pFromY, float pToX, float pToY) {
        tracker.track(TestFrames.withSquare(pFromX, pFromY, SIZE));
        return tracker.track(TestFrames.withSquare(pToX, pToY, SIZE));
    }
}
