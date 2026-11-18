package powerband.editor;

import static powerband.util.IConstants.ERR_NULL_LIBRARY_DEPENDENCY;

import java.util.Optional;

import powerband.exception.RecordingNotFoundException;
import powerband.exception.StorageException;
import powerband.model.IndexEntry;
import powerband.model.ListEntry;
import powerband.model.RecordingHeader;
import powerband.storage.RecordingIndex;
import powerband.storage.RecordingStorage;

/**
 * Grabaciones disponibles para cargar: la lista cronológica sale de
 * recordings.list y la carga por nombre usa la búsqueda binaria de recordings.index.
 *
 * <p>No copia la lista a una colección: la ventana pide las entradas una por una.</p>
 */
public class RecordingLibrary {

    private final RecordingStorage storage;
    private final RecordingIndex index;

    public RecordingLibrary(RecordingStorage pStorage, RecordingIndex pIndex) {
        if (pStorage == null || pIndex == null) {
            throw new IllegalArgumentException(ERR_NULL_LIBRARY_DEPENDENCY);
        }
        storage = pStorage;
        index = pIndex;
    }

    /** Cantidad de grabaciones guardadas. */
    public long listCount() throws StorageException {
        return storage.listCount();
    }

    /** Grabación número {@code pIndex} en orden cronológico (0 = la más vieja). */
    public ListEntry listEntry(long pIndex) throws StorageException {
        return storage.readListEntry(pIndex);
    }

    /**
     * Busca el nombre en el índice (búsqueda binaria en disco) y lee su encabezado.
     *
     * @throws RecordingNotFoundException si no hay ninguna grabación con ese nombre
     */
    public RecordingHeader load(String pName) throws StorageException, RecordingNotFoundException {
        Optional<IndexEntry> entry = index.find(pName);
        if (entry.isEmpty()) {
            throw new RecordingNotFoundException(pName);
        }
        return storage.readHeader(entry.get().getHeaderPosition());
    }
}
