package powerband.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.INDEX_RECORD_SIZE;
import static powerband.util.IConstants.MERGE_TEMP_FILES;
import static powerband.util.IConstants.NAME_LENGTH;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.exception.StorageException;
import powerband.model.IndexEntry;

class RecordingIndexTest {

    private static final long SEED = 42L;
    private static final int MANY = 100;
    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @TempDir
    File tempDir;

    private File indexFile;
    private RecordingIndex index;

    @BeforeEach
    void setUp() {
        indexFile = new File(tempDir, "recordings.index");
        index = new RecordingIndex(indexFile, tempDir);
    }

    @Test
    void emptyIndexHasNoEntries() throws Exception {
        assertEquals(0L, index.count());
        assertTrue(index.find("cualquiera").isEmpty());
    }

    @Test
    void addOneEntryCanBeFound() throws Exception {
        index.add("Cancion1", 640L);

        Optional<IndexEntry> found = index.find("Cancion1");
        assertTrue(found.isPresent());
        assertEquals("Cancion1", found.get().getName());
        assertEquals(640L, found.get().getHeaderPosition());
    }

    @Test
    void recordsHaveFixedSize() throws Exception {
        addAll(List.of("delta", "a", "Una cancion bastante larga", "b"));

        assertEquals(4L, index.count());
        assertEquals(index.count() * INDEX_RECORD_SIZE, indexFile.length());
    }

    @Test
    void entriesAreSortedAfterEachAdd() throws Exception {
        List<String> added = new ArrayList<>();
        for (String name : List.of("mango", "fresa", "zanahoria", "apio", "kiwi", "banano", "uva")) {
            index.add(name, positionOf(added.size()));
            added.add(name);
            assertSortedAndComplete(added);
        }
    }

    @Test
    void sortsReverseOrderInput() throws Exception {
        List<String> names = List.of("g", "f", "e", "d", "c", "b", "a");
        addAll(names);

        assertSortedAndComplete(names);
    }

    @Test
    void sortsAlreadySortedInput() throws Exception {
        List<String> names = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        addAll(names);

        assertSortedAndComplete(names);
    }

    @Test
    void sortsCountsThatAreNotPowersOfTwo() throws Exception {
        for (int count : new int[] {3, 5, 7, 13}) {
            File file = new File(tempDir, "count" + count + ".index");
            index = new RecordingIndex(file, tempDir);
            List<String> names = randomNames(count, SEED + count);
            addAll(names);

            assertSortedAndComplete(names);
        }
    }

    @Test
    void orderIsCaseSensitive() throws Exception {
        index.add("cancion1", 64L);
        index.add("Cancion1", 128L);
        index.add("CANCION1", 192L);

        assertEquals("CANCION1", index.readEntry(0).getName());
        assertEquals("Cancion1", index.readEntry(1).getName());
        assertEquals("cancion1", index.readEntry(2).getName());
        assertEquals(64L, index.find("cancion1").get().getHeaderPosition());
        assertEquals(128L, index.find("Cancion1").get().getHeaderPosition());
        assertEquals(192L, index.find("CANCION1").get().getHeaderPosition());
    }

    @Test
    void findsFirstMiddleAndLast() throws Exception {
        Map<String, Long> positions = addAll(List.of("c", "a", "e", "b", "d"));

        for (String name : List.of("a", "c", "e")) {
            assertEquals(positions.get(name), index.find(name).get().getHeaderPosition());
        }
    }

    @Test
    void findsEveryEntryOfManyRecords() throws Exception {
        Map<String, Long> positions = addAll(randomNames(MANY, SEED));

        assertEquals(MANY, index.count());
        for (Map.Entry<String, Long> entry : positions.entrySet()) {
            Optional<IndexEntry> found = index.find(entry.getKey());
            assertTrue(found.isPresent(), "no se encontró " + entry.getKey());
            assertEquals(entry.getValue(), found.get().getHeaderPosition());
        }
    }

    @Test
    void missingNameIsEmpty() throws Exception {
        addAll(List.of("b", "d", "f"));

        assertFalse(index.find("a").isPresent());
        assertFalse(index.find("c").isPresent());
        assertFalse(index.find("z").isPresent());
    }

    @Test
    void nameWithMaxLengthIsKept() throws Exception {
        String longName = "x".repeat(NAME_LENGTH);
        addAll(List.of("m", longName, "a"));

        assertTrue(index.find(longName).isPresent());
        assertEquals(longName, index.readEntry(2).getName());
    }

    @Test
    void noTempFilesRemainAfterAdd() throws Exception {
        addAll(List.of("c", "a", "b", "e", "d"));

        for (String temp : MERGE_TEMP_FILES) {
            assertFalse(new File(tempDir, temp).exists(), "quedó " + temp);
        }
        String[] files = tempDir.list();
        assertArrayEquals(new String[] {"recordings.index"}, files);
    }

    @Test
    void failedSortLeavesIndexUnchanged() throws Exception {
        addAll(List.of("b", "a"));
        byte[] before = Files.readAllBytes(indexFile.toPath());
        File notADirectory = new File(tempDir, "no-es-carpeta");
        Files.writeString(notADirectory.toPath(), "archivo");
        RecordingIndex broken = new RecordingIndex(indexFile, notADirectory);

        assertThrows(StorageException.class, () -> broken.add("c", 999L));

        assertArrayEquals(before, Files.readAllBytes(indexFile.toPath()));
        assertTrue(index.find("c").isEmpty());
    }

    // ------------------------------------------------------------ auxiliares

    /** Agrega los nombres en ese orden con posiciones distintas y las devuelve. */
    private Map<String, Long> addAll(List<String> pNames) throws StorageException {
        Map<String, Long> positions = new LinkedHashMap<>();
        for (String name : pNames) {
            long position = positionOf(positions.size());
            index.add(name, position);
            positions.put(name, position);
        }
        return positions;
    }

    private static long positionOf(int pOrder) {
        return (pOrder + 1L) * 64L;
    }

    private void assertSortedAndComplete(List<String> pExpectedNames) throws StorageException {
        List<String> expected = new ArrayList<>(pExpectedNames);
        Collections.sort(expected);
        List<String> actual = new ArrayList<>();
        for (long i = 0; i < index.count(); i++) {
            actual.add(index.readEntry(i).getName());
        }
        assertEquals(expected, actual);
    }

    /** Nombres distintos de 1 a NAME_LENGTH caracteres, sin espacios. */
    private static List<String> randomNames(int pCount, long pSeed) {
        Random random = new Random(pSeed);
        List<String> names = new ArrayList<>();
        while (names.size() < pCount) {
            int length = 1 + random.nextInt(NAME_LENGTH);
            StringBuilder name = new StringBuilder();
            for (int i = 0; i < length; i++) {
                name.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
            }
            if (!names.contains(name.toString())) {
                names.add(name.toString());
            }
        }
        return names;
    }
}
