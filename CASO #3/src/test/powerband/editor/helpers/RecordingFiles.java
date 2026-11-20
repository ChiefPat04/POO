package powerband.editor.helpers;

import static powerband.util.IConstants.PENDING_POSITION;
import static powerband.util.IConstants.RECORD_SIZE;

import java.io.File;

import powerband.editor.RecordingLibrary;
import powerband.exception.StorageException;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.model.SynthEvent;
import powerband.storage.RecordingIndex;
import powerband.storage.RecordingStorage;

/**
 * Archivos de grabaciones reales en una carpeta de prueba, con un atajo para
 * guardar una grabación completa (encabezado, comandos, lista e índice).
 */
public class RecordingFiles {

    private final RecordingStorage storage;
    private final RecordingIndex index;

    public RecordingFiles(File pDir) {
        storage = new RecordingStorage(new File(pDir, "recordings.music"),
                new File(pDir, "recordings.list"));
        index = new RecordingIndex(new File(pDir, "recordings.index"), pDir);
    }

    /** Guarda una grabación con esos comandos (cada uno con su {@code timeMs} relativo). */
    public RecordingHeader save(String pName, SynthEvent... pEvents) throws StorageException {
        long headerPosition = storage.appendHeader(
                new RecordingHeader("", PENDING_POSITION, PENDING_POSITION));
        for (SynthEvent event : pEvents) {
            storage.appendEvent(event);
        }
        RecordingHeader header = new RecordingHeader(pName, headerPosition + RECORD_SIZE,
                storage.musicLength());
        storage.writeHeader(headerPosition, header);
        storage.appendListEntry(new ListEntry(pName, System.currentTimeMillis()));
        index.add(pName, headerPosition);
        return header;
    }

    public RecordingStorage getStorage() {
        return storage;
    }

    public RecordingIndex getIndex() {
        return index;
    }

    public RecordingLibrary library() {
        return new RecordingLibrary(storage, index);
    }
}
