package powerband.editor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.FILE_START;
import static powerband.util.IConstants.PENDING_POSITION;
import static powerband.util.IConstants.RECORD_SIZE;
import static powerband.util.IConstants.SC_HOST;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.command.BassCommand;
import powerband.command.OrganCommand;
import powerband.command.PianoCommand;
import powerband.command.helpers.OscReceiver;
import powerband.editor.helpers.FakeIndexer;
import powerband.exception.DuplicateRecordingNameException;
import powerband.exception.StorageException;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;
import powerband.storage.RecordingStorage;

class RecorderTest {

    @TempDir
    File tempDir;

    private File musicFile;
    private File listFile;
    private RecordingStorage storage;

    @BeforeEach
    void setUp() {
        musicFile = new File(tempDir, "recordings.music");
        listFile = new File(tempDir, "recordings.list");
        storage = new RecordingStorage(musicFile, listFile);
    }

    /** Receptor, cliente, los 3 comandos y un grabador; se cierra con try-with-resources. */
    private static final class Band implements AutoCloseable {

        private final OscReceiver receiver;
        private final OscClient client;
        private final BassCommand bass;
        private final OrganCommand organ;
        private final FakeIndexer indexer = new FakeIndexer();
        private final Recorder recorder;

        Band(RecordingStorage pStorage) throws Exception {
            receiver = new OscReceiver();
            client = new OscClient(SC_HOST, receiver.getPort());
            bass = new BassCommand(client);
            organ = new OrganCommand(client);
            recorder = new Recorder(pStorage, indexer, new PianoCommand(client), bass, organ);
        }

        @Override
        public void close() {
            client.close();
            receiver.close();
        }
    }

    @Test
    void ignoresEventsBeforeStartAndAfterStop() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.piano(62, 500, 4));
            band.recorder.stop();
            band.recorder.onSynthEvent(SynthEvent.piano(64, 500, 4));

            assertEquals(2L * RECORD_SIZE, storage.musicLength());
            assertEquals(62, storage.readEvent(RECORD_SIZE).getNote());
        }
    }

    @Test
    void startWritesProvisionalHeader() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();

            RecordingHeader header = storage.readHeader(FILE_START);
            assertTrue(band.recorder.isRecording());
            assertEquals("", header.getName());
            assertEquals(RECORD_SIZE, header.getStartPosition());
            assertEquals(PENDING_POSITION, header.getEndPosition());
        }
    }

    @Test
    void recordsRelativeTime() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.bass(36, 450)
                    .withTimeMs(band.recorder.getStartTimeMs() + 500L));

            assertEquals(500L, storage.readEvent(RECORD_SIZE).getTimeMs());
        }
    }

    @Test
    void clampsNegativeRelativeTimeToZero() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.bass(36, 450)
                    .withTimeMs(band.recorder.getStartTimeMs() - 100L));

            assertEquals(0L, storage.readEvent(RECORD_SIZE).getTimeMs());
        }
    }

    @Test
    void stopRecordsNoteOffForEveryActiveOrganNote() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.organ.execute(SynthEvent.organOn(60));
            band.organ.execute(SynthEvent.organOn(64));
            band.bass.execute(SynthEvent.bass(36, 450));
            band.recorder.stop();

            // encabezado + 2 noteOn + bajo + 2 noteOff
            assertEquals(6L * RECORD_SIZE, storage.musicLength());
            SynthEvent first = storage.readEvent(4L * RECORD_SIZE);
            SynthEvent second = storage.readEvent(5L * RECORD_SIZE);
            assertEquals(SynthAction.NOTE_OFF, first.getAction());
            assertEquals(SynthAction.NOTE_OFF, second.getAction());
            assertEquals(Set.of(60, 64), Set.of(first.getNote(), second.getNote()));
            assertTrue(band.recorder.isPendingName());
        }
    }

    @Test
    void saveRewritesHeaderAppendsListEntryAndCallsIndexer() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
            band.recorder.stop();
            long before = System.currentTimeMillis();
            band.recorder.save("Cancion1");
            long after = System.currentTimeMillis();

            RecordingHeader header = storage.readHeader(FILE_START);
            assertEquals("Cancion1", header.getName());
            assertEquals(RECORD_SIZE, header.getStartPosition());
            assertEquals(2L * RECORD_SIZE, header.getEndPosition());
            assertEquals(2L * RECORD_SIZE, storage.musicLength());

            assertEquals(1L, storage.listCount());
            ListEntry entry = storage.readListEntry(0);
            assertEquals("Cancion1", entry.getName());
            assertTrue(entry.getCreatedAtMs() >= before && entry.getCreatedAtMs() <= after);

            assertEquals(List.of("Cancion1"), band.indexer.getNames());
            assertEquals(List.of(FILE_START), band.indexer.getPositions());
            assertFalse(band.recorder.isPendingName());
        }
    }

    @Test
    void saveWithDuplicateNameThrowsAndAllowsRetry() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.stop();
            band.recorder.save("Cancion1");

            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
            band.recorder.stop();
            byte[] musicBefore = Files.readAllBytes(musicFile.toPath());
            byte[] listBefore = Files.readAllBytes(listFile.toPath());

            assertThrows(DuplicateRecordingNameException.class,
                    () -> band.recorder.save("Cancion1"));

            assertArrayEquals(musicBefore, Files.readAllBytes(musicFile.toPath()));
            assertArrayEquals(listBefore, Files.readAllBytes(listFile.toPath()));
            assertEquals(1, band.indexer.getNames().size());
            assertTrue(band.recorder.isPendingName());

            band.recorder.save("Cancion2");

            assertEquals(List.of("Cancion1", "Cancion2"), band.indexer.getNames());
            assertEquals(List.of(FILE_START, (long) RECORD_SIZE), band.indexer.getPositions());
            assertEquals("Cancion2", storage.readHeader(RECORD_SIZE).getName());
            assertEquals(2L, storage.listCount());
        }
    }

    @Test
    void saveWithInvalidNameThrowsIllegalArgumentException() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.stop();

            for (String name : Arrays.asList(null, "", "canción", "año", " a", "a ",
                    "a".repeat(33))) {
                assertThrows(IllegalArgumentException.class, () -> band.recorder.save(name));
            }

            assertTrue(band.recorder.isPendingName());
            assertFalse(listFile.exists());
            assertTrue(band.indexer.getNames().isEmpty());
        }
    }

    @Test
    void namesDifferingOnlyInCaseAreBothAccepted() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.stop();
            band.recorder.save("Cancion1");
            band.recorder.start();
            band.recorder.stop();
            band.recorder.save("cancion1");

            assertEquals(List.of("Cancion1", "cancion1"), band.indexer.getNames());
            assertEquals(2L, storage.listCount());
        }
    }

    @Test
    void discardRestoresMusicFileLength() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
            band.recorder.stop();
            band.recorder.save("Cancion1");
            long lengthBefore = storage.musicLength();

            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.piano(62, 500, 4));
            band.recorder.stop();
            band.recorder.discard();

            assertEquals(lengthBefore, storage.musicLength());
            assertFalse(band.recorder.isPendingName());
            assertEquals(1L, storage.listCount());
            assertEquals(1, band.indexer.getNames().size());
        }
    }

    @Test
    void secondRecordingStartsAfterFirstAndLeavesItUntouched() throws Exception {
        try (Band band = new Band(storage)) {
            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
            band.recorder.stop();
            band.recorder.save("primera");
            byte[] firstRecording = Files.readAllBytes(musicFile.toPath());

            band.recorder.start();
            band.recorder.onSynthEvent(SynthEvent.bass(36, 450));
            band.recorder.stop();
            band.recorder.save("segunda");

            long secondHeader = 2L * RECORD_SIZE;
            byte[] all = Files.readAllBytes(musicFile.toPath());
            assertArrayEquals(firstRecording, Arrays.copyOf(all, firstRecording.length));
            assertEquals(List.of(FILE_START, secondHeader), band.indexer.getPositions());
            RecordingHeader header = storage.readHeader(secondHeader);
            assertEquals("segunda", header.getName());
            assertEquals(secondHeader + RECORD_SIZE, header.getStartPosition());
            assertEquals(secondHeader + 2L * RECORD_SIZE, header.getEndPosition());
        }
    }

    @Test
    void concurrentEventsAreAllRecordedAsWholeRecords() throws Exception {
        int threads = 4;
        int eventsPerThread = 50;
        try (Band band = new Band(storage)) {
            band.recorder.start();
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < eventsPerThread; i++) {
                        band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            pool.shutdown();
            band.recorder.stop();

            int events = threads * eventsPerThread;
            assertEquals((events + 1L) * RECORD_SIZE, storage.musicLength());
            for (int i = 1; i <= events; i++) {
                assertEquals(60, storage.readEvent((long) i * RECORD_SIZE).getNote());
            }
        }
    }

    @Test
    void stopRethrowsStorageFailureFromOnSynthEvent() throws Exception {
        File dir = new File(tempDir, "sub");
        File music = new File(dir, "recordings.music");
        RecordingStorage broken = new RecordingStorage(music, new File(dir, "recordings.list"));
        try (Band band = new Band(broken)) {
            band.recorder.start();
            // La carpeta padre pasa a ser un archivo: ya no se puede escribir ahí.
            Files.delete(music.toPath());
            Files.delete(dir.toPath());
            Files.createFile(dir.toPath());

            band.recorder.onSynthEvent(SynthEvent.piano(60, 500, 4));
            band.recorder.onSynthEvent(SynthEvent.piano(62, 500, 4));

            StorageException thrown = assertThrows(StorageException.class,
                    () -> band.recorder.stop());
            assertTrue(thrown.getCause() != null);
            assertTrue(band.recorder.isPendingName());
            assertSame(false, band.recorder.isRecording());
        }
    }

    @Test
    void wrongStateCallsThrowIllegalStateException() throws Exception {
        try (Band band = new Band(storage)) {
            assertThrows(IllegalStateException.class, () -> band.recorder.stop());
            assertThrows(IllegalStateException.class, () -> band.recorder.save("Cancion1"));
            assertThrows(IllegalStateException.class, () -> band.recorder.discard());

            band.recorder.start();

            assertThrows(IllegalStateException.class, () -> band.recorder.start());
        }
    }
}
