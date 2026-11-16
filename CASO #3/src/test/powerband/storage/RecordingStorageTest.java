package powerband.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.FILE_START;
import static powerband.util.IConstants.NAME_LENGTH;
import static powerband.util.IConstants.PENDING_POSITION;
import static powerband.util.IConstants.RECORD_SIZE;
import static powerband.util.IConstants.RECORD_TYPE_COMMAND;
import static powerband.util.IConstants.RECORD_TYPE_HEADER;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.exception.StorageException;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.model.SynthEvent;

class RecordingStorageTest {

    private static final float DELTA = 1e-6f;

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

    @Test
    void headerRoundTripKeepsNameAndPositions() throws Exception {
        long position = storage.appendHeader(new RecordingHeader("Cancion1", 64L, 256L));

        RecordingHeader header = storage.readHeader(position);

        assertEquals(FILE_START, position);
        assertEquals("Cancion1", header.getName());
        assertEquals(64L, header.getStartPosition());
        assertEquals(256L, header.getEndPosition());
    }

    @Test
    void shortNameIsPaddedWithSpacesToThirtyTwoBytes() throws Exception {
        storage.appendHeader(new RecordingHeader("abc", RECORD_SIZE, PENDING_POSITION));

        byte[] bytes = Files.readAllBytes(musicFile.toPath());

        String stored = new String(bytes, 1, NAME_LENGTH, StandardCharsets.US_ASCII);
        assertEquals("abc" + " ".repeat(NAME_LENGTH - 3), stored);
        assertEquals("abc", storage.readHeader(FILE_START).getName());
    }

    @Test
    void commandRoundTripKeepsAllFieldsForPianoBassAndOrgan() throws Exception {
        SynthEvent[] events = {
            SynthEvent.piano(60, 1500, 3, 0.4f).withTimeMs(10L),
            SynthEvent.bass(36, 450, 0.6f).withTimeMs(20L),
            SynthEvent.organOn(69, 0.4f, new float[] { 1f, 2.01f, 4f },
                    new float[] { 1f, 0.6f, 0.3f }).withTimeMs(30L),
            SynthEvent.organOff(69).withTimeMs(40L)
        };
        for (SynthEvent event : events) {
            storage.appendEvent(event);
        }

        for (int i = 0; i < events.length; i++) {
            assertSameEvent(events[i], storage.readEvent((long) i * RECORD_SIZE));
        }
    }

    @Test
    void everyRecordMeasuresRecordSize() throws Exception {
        storage.appendHeader(new RecordingHeader("a", RECORD_SIZE, PENDING_POSITION));
        storage.appendEvent(SynthEvent.piano(60, 500, 4));
        storage.appendEvent(SynthEvent.bass(36, 500));
        storage.appendEvent(SynthEvent.organOn(60));

        assertEquals(4L * RECORD_SIZE, musicFile.length());
        assertEquals(4L * RECORD_SIZE, storage.musicLength());
    }

    @Test
    void firstByteIsRecordType() throws Exception {
        storage.appendHeader(new RecordingHeader("a", RECORD_SIZE, PENDING_POSITION));
        storage.appendEvent(SynthEvent.piano(60, 500, 4));
        storage.appendEvent(SynthEvent.organOff(60));

        byte[] bytes = Files.readAllBytes(musicFile.toPath());

        assertEquals(RECORD_TYPE_HEADER, bytes[0]);
        assertEquals(RECORD_TYPE_COMMAND, bytes[RECORD_SIZE]);
        assertEquals(RECORD_TYPE_COMMAND, bytes[2 * RECORD_SIZE]);
    }

    @Test
    void writeHeaderRewritesInPlaceWithoutChangingLength() throws Exception {
        SynthEvent event = SynthEvent.piano(60, 1500, 3).withTimeMs(123L);
        long position = storage.appendHeader(
                new RecordingHeader("", RECORD_SIZE, PENDING_POSITION));
        storage.appendEvent(event);
        long lengthBefore = musicFile.length();

        storage.writeHeader(position, new RecordingHeader("Final", RECORD_SIZE, 2L * RECORD_SIZE));

        assertEquals(lengthBefore, musicFile.length());
        RecordingHeader header = storage.readHeader(position);
        assertEquals("Final", header.getName());
        assertEquals(2L * RECORD_SIZE, header.getEndPosition());
        assertSameEvent(event, storage.readEvent(RECORD_SIZE));
    }

    @Test
    void readHeaderOnCommandRecordThrowsStorageException() throws Exception {
        storage.appendEvent(SynthEvent.piano(60, 500, 4));

        assertThrows(StorageException.class, () -> storage.readHeader(FILE_START));
    }

    @Test
    void readEventOnHeaderRecordThrowsStorageException() throws Exception {
        storage.appendHeader(new RecordingHeader("a", RECORD_SIZE, PENDING_POSITION));

        assertThrows(StorageException.class, () -> storage.readEvent(FILE_START));
    }

    @Test
    void listEntriesAreReadInAppendOrder() throws Exception {
        storage.appendListEntry(new ListEntry("zeta", 1000L));
        storage.appendListEntry(new ListEntry("alfa", 2000L));
        storage.appendListEntry(new ListEntry("media", 3000L));

        assertEquals(3L, storage.listCount());
        assertEquals("zeta", storage.readListEntry(0).getName());
        assertEquals(1000L, storage.readListEntry(0).getCreatedAtMs());
        assertEquals("alfa", storage.readListEntry(1).getName());
        assertEquals(2000L, storage.readListEntry(1).getCreatedAtMs());
        assertEquals("media", storage.readListEntry(2).getName());
        assertEquals(3000L, storage.readListEntry(2).getCreatedAtMs());
    }

    @Test
    void listContainsNameIsCaseSensitive() throws Exception {
        storage.appendListEntry(new ListEntry("Otra", 1L));
        storage.appendListEntry(new ListEntry("Cancion1", 2L));

        assertTrue(storage.listContainsName("Cancion1"));
        assertFalse(storage.listContainsName("cancion1"));
        assertFalse(storage.listContainsName("Cancion"));
    }

    @Test
    void listContainsNameReturnsFalseWhenFileDoesNotExist() throws Exception {
        assertFalse(storage.listContainsName("Cancion1"));
    }

    @Test
    void truncateMusicRemovesTrailingRecords() throws Exception {
        storage.appendHeader(new RecordingHeader("a", RECORD_SIZE, PENDING_POSITION));
        storage.appendEvent(SynthEvent.piano(60, 500, 4));
        storage.appendEvent(SynthEvent.bass(36, 500));

        storage.truncateMusic(RECORD_SIZE);

        assertEquals(RECORD_SIZE, musicFile.length());
        assertEquals("a", storage.readHeader(FILE_START).getName());
    }

    @Test
    void isValidNameRejectsNullEmptyTooLongNonAsciiAndEdgeSpaces() {
        assertTrue(RecordingStorage.isValidName("Cancion1"));
        assertTrue(RecordingStorage.isValidName("Mi cancion"));
        assertTrue(RecordingStorage.isValidName("a".repeat(NAME_LENGTH)));

        assertFalse(RecordingStorage.isValidName(null));
        assertFalse(RecordingStorage.isValidName(""));
        assertFalse(RecordingStorage.isValidName("a".repeat(NAME_LENGTH + 1)));
        assertFalse(RecordingStorage.isValidName("canción"));
        assertFalse(RecordingStorage.isValidName("año"));
        assertFalse(RecordingStorage.isValidName(" a"));
        assertFalse(RecordingStorage.isValidName("a "));
    }

    @Test
    void createsDataDirectoryWhenMissing() throws Exception {
        File dataDir = new File(tempDir, "data");
        RecordingStorage nested = new RecordingStorage(
                new File(dataDir, "recordings.music"), new File(dataDir, "recordings.list"));

        nested.appendHeader(new RecordingHeader("a", RECORD_SIZE, PENDING_POSITION));
        nested.appendListEntry(new ListEntry("a", 1L));

        assertTrue(dataDir.isDirectory());
        assertEquals(RECORD_SIZE, nested.musicLength());
        assertEquals(1L, nested.listCount());
    }

    private static void assertSameEvent(SynthEvent pExpected, SynthEvent pActual) {
        assertEquals(pExpected.getTimeMs(), pActual.getTimeMs());
        assertEquals(pExpected.getSynth(), pActual.getSynth());
        assertEquals(pExpected.getAction(), pActual.getAction());
        assertEquals(pExpected.getNote(), pActual.getNote());
        assertEquals(pExpected.getDurationMs(), pActual.getDurationMs());
        assertEquals(pExpected.getThird(), pActual.getThird());
        assertEquals(pExpected.getAmp(), pActual.getAmp(), DELTA);
        assertEquals(pExpected.getR1(), pActual.getR1(), DELTA);
        assertEquals(pExpected.getR2(), pActual.getR2(), DELTA);
        assertEquals(pExpected.getR3(), pActual.getR3(), DELTA);
        assertEquals(pExpected.getA1(), pActual.getA1(), DELTA);
        assertEquals(pExpected.getA2(), pActual.getA2(), DELTA);
        assertEquals(pExpected.getA3(), pActual.getA3(), DELTA);
    }
}
