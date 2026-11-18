package powerband.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.PLAYER_TIMING_TOLERANCE_MS;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.editor.helpers.RecordingFiles;
import powerband.editor.helpers.TimedCommand;
import powerband.model.RecordingHeader;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;

class PlayerTest {

    private static final long WAIT_TIMEOUT_MS = 3000L;
    private static final long WAIT_STEP_MS = 5L;

    @TempDir
    File tempDir;

    private RecordingFiles files;
    private TimedCommand piano;
    private TimedCommand bass;
    private TimedCommand organ;
    private Player player;
    private final List<String> finished = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() {
        files = new RecordingFiles(tempDir);
        piano = new TimedCommand();
        bass = new TimedCommand();
        organ = new TimedCommand();
        player = new Player(files.getStorage(), piano, bass, organ);
        player.setListener(finished::add);
    }

    @Test
    void sendsEachEventToItsSynthInOrder() throws Exception {
        RecordingHeader header = files.save("Mezcla",
                SynthEvent.piano(60, 500, 4, 0.4f).withTimeMs(0L),
                SynthEvent.bass(36, 450, 0.5f).withTimeMs(10L),
                SynthEvent.organOn(64, 0.3f).withTimeMs(20L),
                SynthEvent.organOff(64).withTimeMs(30L));

        player.play(header);

        assertEquals(1, piano.getEvents().size());
        SynthEvent chord = piano.getEvents().get(0);
        assertEquals(60, chord.getNote());
        assertEquals(500, chord.getDurationMs());
        assertEquals(4, chord.getThird());
        assertEquals(0.4f, chord.getAmp(), 0.0001f);
        assertEquals(1, bass.getEvents().size());
        assertEquals(36, bass.getEvents().get(0).getNote());
        assertEquals(450, bass.getEvents().get(0).getDurationMs());
        assertEquals(2, organ.getEvents().size());
        assertEquals(SynthAction.NOTE_ON, organ.getEvents().get(0).getAction());
        assertEquals(SynthAction.NOTE_OFF, organ.getEvents().get(1).getAction());
        assertTrue(piano.getTimes().get(0) <= bass.getTimes().get(0));
        assertTrue(bass.getTimes().get(0) <= organ.getTimes().get(0));
    }

    @Test
    void playsOnlyTheChosenRecording() throws Exception {
        files.save("Primera", SynthEvent.bass(30, 400).withTimeMs(0L));
        RecordingHeader second = files.save("Segunda", SynthEvent.piano(67, 500, 3).withTimeMs(0L));

        player.play(second);

        assertTrue(bass.getEvents().isEmpty());
        assertEquals(67, piano.getEvents().get(0).getNote());
    }

    @Test
    void keepsRecordedIntervals() throws Exception {
        RecordingHeader header = files.save("Ritmo",
                SynthEvent.piano(60, 300, 4).withTimeMs(0L),
                SynthEvent.piano(62, 300, 4).withTimeMs(100L),
                SynthEvent.piano(64, 300, 4).withTimeMs(250L));

        player.play(header);

        List<Long> times = piano.getTimes();
        assertEquals(100L, times.get(1) - times.get(0), PLAYER_TIMING_TOLERANCE_MS);
        assertEquals(150L, times.get(2) - times.get(1), PLAYER_TIMING_TOLERANCE_MS);
    }

    @Test
    void timingDoesNotDependOnLoadTime() throws Exception {
        files.save("Lenta",
                SynthEvent.piano(60, 300, 4).withTimeMs(0L),
                SynthEvent.piano(62, 300, 4).withTimeMs(100L));
        RecordingHeader header = files.library().load("Lenta");
        Thread.sleep(200L);

        long before = System.currentTimeMillis();
        player.play(header);

        List<Long> times = piano.getTimes();
        assertEquals(0L, times.get(0) - before, PLAYER_TIMING_TOLERANCE_MS);
        assertEquals(100L, times.get(1) - times.get(0), PLAYER_TIMING_TOLERANCE_MS);
    }

    @Test
    void emptyRecordingSendsNothing() throws Exception {
        RecordingHeader header = files.save("Vacia");

        player.play(header);

        assertTrue(piano.getEvents().isEmpty());
        assertTrue(bass.getEvents().isEmpty());
        assertTrue(organ.getEvents().isEmpty());
    }

    @Test
    void startRunsInBackgroundAndNotifiesWhenDone() throws Exception {
        RecordingHeader header = files.save("Fondo",
                SynthEvent.piano(60, 300, 4).withTimeMs(0L),
                SynthEvent.piano(62, 300, 4).withTimeMs(300L));
        try {
            player.start(header);
            assertTrue(piano.getEvents().size() < 2, "start no debe esperar a que termine");
            awaitTrue(() -> !finished.isEmpty());
        } finally {
            player.stop();
        }

        assertFalse(player.isPlaying());
        assertEquals(List.of("Fondo"), finished);
        assertEquals(2, piano.getEvents().size());
    }

    @Test
    void stopTurnsOffHeldOrganNotes() throws Exception {
        RecordingHeader header = files.save("Sostenida",
                SynthEvent.organOn(64, 0.3f).withTimeMs(0L),
                SynthEvent.organOff(64).withTimeMs(2000L),
                SynthEvent.piano(60, 300, 4).withTimeMs(2000L));
        try {
            player.start(header);
            awaitTrue(() -> organ.getEvents().size() == 1);
            Thread.sleep(100L);
        } finally {
            player.stop();
        }
        awaitTrue(() -> !finished.isEmpty());

        assertEquals(2, organ.getEvents().size());
        assertEquals(SynthAction.NOTE_OFF, organ.getEvents().get(1).getAction());
        assertEquals(64, organ.getEvents().get(1).getNote());
        assertTrue(piano.getEvents().isEmpty());
        assertEquals(List.of("Sostenida"), finished);
    }

    @Test
    void startWhilePlayingThrows() throws Exception {
        RecordingHeader header = files.save("Larga",
                SynthEvent.piano(60, 300, 4).withTimeMs(0L),
                SynthEvent.piano(62, 300, 4).withTimeMs(1000L));
        try {
            player.start(header);
            assertThrows(IllegalStateException.class, () -> player.start(header));
        } finally {
            player.stop();
        }
    }

    @Test
    void stopWithoutPlayingDoesNotFail() {
        player.stop();
        player.stop();

        assertFalse(player.isPlaying());
    }

    @Test
    void sendFailureDoesNotStopPlayback() throws Exception {
        bass.setFailing(true);
        RecordingHeader header = files.save("Falla",
                SynthEvent.bass(36, 400).withTimeMs(0L),
                SynthEvent.piano(60, 300, 4).withTimeMs(10L),
                SynthEvent.piano(62, 300, 4).withTimeMs(20L));

        player.play(header);

        assertEquals(2, piano.getEvents().size());
    }

    // ------------------------------------------------------------ auxiliares

    private interface Condition {
        boolean holds();
    }

    private static void awaitTrue(Condition pCondition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (!pCondition.holds() && System.currentTimeMillis() < deadline) {
            Thread.sleep(WAIT_STEP_MS);
        }
        assertTrue(pCondition.holds(), "no pasó a tiempo");
    }
}
