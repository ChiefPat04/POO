package powerband.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.editor.helpers.RecordingFiles;
import powerband.exception.RecordingNotFoundException;
import powerband.model.RecordingHeader;
import powerband.model.SynthEvent;

class RecordingLibraryTest {

    @TempDir
    File tempDir;

    private RecordingFiles files;
    private RecordingLibrary library;

    @BeforeEach
    void setUp() {
        files = new RecordingFiles(tempDir);
        library = files.library();
    }

    @Test
    void listsRecordingsInChronologicalOrder() throws Exception {
        files.save("Zeta", SynthEvent.piano(60, 500, 4).withTimeMs(0L));
        files.save("alfa", SynthEvent.piano(62, 500, 4).withTimeMs(0L));
        files.save("Media", SynthEvent.piano(64, 500, 4).withTimeMs(0L));

        assertEquals(3L, library.listCount());
        assertEquals("Zeta", library.listEntry(0).getName());
        assertEquals("alfa", library.listEntry(1).getName());
        assertEquals("Media", library.listEntry(2).getName());
    }

    @Test
    void loadReturnsHeaderOfSavedRecording() throws Exception {
        files.save("Primera", SynthEvent.piano(60, 500, 4).withTimeMs(0L));
        RecordingHeader saved = files.save("Segunda",
                SynthEvent.bass(36, 400).withTimeMs(0L),
                SynthEvent.bass(38, 400).withTimeMs(100L));

        RecordingHeader loaded = library.load("Segunda");

        assertEquals("Segunda", loaded.getName());
        assertEquals(saved.getStartPosition(), loaded.getStartPosition());
        assertEquals(saved.getEndPosition(), loaded.getEndPosition());
    }

    @Test
    void loadUnknownNameThrows() throws Exception {
        files.save("Existe", SynthEvent.piano(60, 500, 4).withTimeMs(0L));

        assertThrows(RecordingNotFoundException.class, () -> library.load("NoExiste"));
    }

    @Test
    void loadIsCaseSensitive() throws Exception {
        files.save("Cancion1", SynthEvent.piano(60, 500, 4).withTimeMs(0L));

        assertThrows(RecordingNotFoundException.class, () -> library.load("cancion1"));
        assertEquals("Cancion1", library.load("Cancion1").getName());
    }
}
