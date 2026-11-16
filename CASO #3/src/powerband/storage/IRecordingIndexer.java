package powerband.storage;

import powerband.exception.StorageException;

/**
 * Índice de grabaciones por nombre. El grabador lo avisa al cerrar cada grabación;
 * la implementación (Balanced Merge Sort sobre recordings.index) es de ft-6.
 */
public interface IRecordingIndexer {

    void add(String pName, long pHeaderPosition) throws StorageException;
}
