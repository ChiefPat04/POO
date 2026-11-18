package powerband.storage;

import static powerband.util.IConstants.DATA_DIR;
import static powerband.util.IConstants.EMPTY_COUNT;
import static powerband.util.IConstants.ERR_INDEX_OUT_OF_RANGE;
import static powerband.util.IConstants.ERR_INDEX_READ;
import static powerband.util.IConstants.ERR_INDEX_SORT;
import static powerband.util.IConstants.ERR_INDEX_WRITE;
import static powerband.util.IConstants.ERR_NULL_INDEX_FILE;
import static powerband.util.IConstants.ERR_TEMP_DELETE;
import static powerband.util.IConstants.FILE_MODE_READ;
import static powerband.util.IConstants.FILE_MODE_READ_WRITE;
import static powerband.util.IConstants.FIRST_INDEX;
import static powerband.util.IConstants.INDEX_FILE;
import static powerband.util.IConstants.INDEX_RECORD_SIZE;
import static powerband.util.IConstants.MERGE_INITIAL_RUN;
import static powerband.util.IConstants.MERGE_RUN_GROWTH;
import static powerband.util.IConstants.MERGE_SECOND_OFFSET;
import static powerband.util.IConstants.MERGE_TEMP_FILES;
import static powerband.util.IConstants.MERGE_WAYS;
import static powerband.util.IConstants.NAME_LENGTH;
import static powerband.util.IConstants.SAME_NAME;
import static powerband.util.IConstants.SEARCH_HALF;
import static powerband.util.IConstants.SEARCH_STEP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import powerband.exception.StorageException;
import powerband.model.IndexEntry;

/**
 * Índice de grabaciones por nombre en recordings.index: registros de
 * {@code INDEX_RECORD_SIZE} bytes (nombre + posición del encabezado) ordenados por nombre.
 *
 * <p>Cada {@link #add} vuelve a ordenar el archivo con Balanced Merge Sort de 2 vías
 * y 4 archivos temporales, con tramos iniciales de 1 registro. {@link #find} hace
 * búsqueda binaria en disco. Todo ocurre en disco: en memoria solo están los 2
 * registros que se comparan en la mezcla.</p>
 */
public class RecordingIndex implements IRecordingIndexer {

    private final File indexFile;
    private final File[] tempFiles;

    public RecordingIndex() {
        this(new File(INDEX_FILE), new File(DATA_DIR));
    }

    public RecordingIndex(File pIndexFile, File pTempDir) {
        if (pIndexFile == null || pTempDir == null) {
            throw new IllegalArgumentException(ERR_NULL_INDEX_FILE);
        }
        indexFile = pIndexFile;
        tempFiles = new File[MERGE_TEMP_FILES.size()];
        for (int i = FIRST_INDEX; i < tempFiles.length; i++) {
            tempFiles[i] = new File(pTempDir, MERGE_TEMP_FILES.get(i));
        }
    }

    /**
     * Agrega la grabación al final del índice y lo vuelve a ordenar. Si el
     * ordenamiento falla, el índice queda como estaba antes de la llamada.
     */
    @Override
    public synchronized void add(String pName, long pHeaderPosition) throws StorageException {
        long previousLength = indexFile.length();
        append(new IndexEntry(pName, pHeaderPosition));
        try {
            sort();
        } catch (StorageException e) {
            truncate(previousLength);
            throw e;
        }
    }

    /** Búsqueda binaria en disco: lee un solo registro por paso. */
    public synchronized Optional<IndexEntry> find(String pName) throws StorageException {
        long count = count();
        if (count == EMPTY_COUNT) {
            return Optional.empty();
        }
        try (RandomAccessFile file = new RandomAccessFile(indexFile, FILE_MODE_READ)) {
            long low = EMPTY_COUNT;
            long high = count - SEARCH_STEP;
            while (low <= high) {
                long middle = low + (high - low) / SEARCH_HALF;
                IndexEntry entry = readAt(file, middle);
                int comparison = entry.getName().compareTo(pName);
                if (comparison == SAME_NAME) {
                    return Optional.of(entry);
                }
                if (comparison < SAME_NAME) {
                    low = middle + SEARCH_STEP;
                } else {
                    high = middle - SEARCH_STEP;
                }
            }
            return Optional.empty();
        } catch (IOException e) {
            throw new StorageException(ERR_INDEX_READ + indexFile.getPath(), e);
        }
    }

    /** Cantidad de registros del índice; 0 si el archivo no existe. */
    public synchronized long count() throws StorageException {
        return indexFile.length() / INDEX_RECORD_SIZE;
    }

    /** Lee el registro que está en esa posición lógica (0 = el primero). */
    public synchronized IndexEntry readEntry(long pIndex) throws StorageException {
        if (pIndex < EMPTY_COUNT || pIndex >= count()) {
            throw new IllegalArgumentException(ERR_INDEX_OUT_OF_RANGE + pIndex);
        }
        try (RandomAccessFile file = new RandomAccessFile(indexFile, FILE_MODE_READ)) {
            return readAt(file, pIndex);
        } catch (IOException e) {
            throw new StorageException(ERR_INDEX_READ + indexFile.getPath(), e);
        }
    }

    // ------------------------------------------------------------ Balanced Merge Sort

    /**
     * Ordena recordings.index en disco: reparte los registros en tramos de 1 entre
     * los dos primeros temporales y mezcla de un par al otro, duplicando el largo
     * del tramo, hasta que queda un solo tramo.
     */
    private void sort() throws StorageException {
        if (count() < MERGE_WAYS) {
            return;
        }
        try {
            int inputs = FIRST_INDEX;
            int outputs = MERGE_WAYS;
            distribute(tempFiles[inputs], tempFiles[inputs + MERGE_SECOND_OFFSET]);
            long runLength = MERGE_INITIAL_RUN;
            long leftInSecond;
            do {
                leftInSecond = mergePass(inputs, outputs, runLength);
                runLength *= MERGE_RUN_GROWTH;
                int swap = inputs;
                inputs = outputs;
                outputs = swap;
            } while (leftInSecond > EMPTY_COUNT);
            // después del intercambio, el par de entradas es donde quedó el resultado
            Files.copy(tempFiles[inputs].toPath(), indexFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException(ERR_INDEX_SORT + indexFile.getPath(), e);
        } finally {
            deleteTempFiles();
        }
    }

    /** Copia el índice registro por registro, alternando entre los dos archivos (tramos de 1). */
    private void distribute(File pFirst, File pSecond) throws IOException {
        try (RunReader in = new RunReader(indexFile);
                DataOutputStream first = openWriter(pFirst);
                DataOutputStream second = openWriter(pSecond)) {
            boolean toFirst = true;
            while (in.hasCurrent()) {
                writeEntry(toFirst ? first : second, in.current());
                in.advance();
                toFirst = !toFirst;
            }
        }
    }

    /**
     * Una pasada: mezcla un tramo de cada entrada en la salida actual y alterna la
     * salida, hasta acabar las dos entradas.
     *
     * @return registros escritos en la segunda salida (0 = ya quedó todo ordenado)
     */
    private long mergePass(int pInputs, int pOutputs, long pRunLength) throws IOException {
        long writtenInSecond = EMPTY_COUNT;
        try (RunReader a = new RunReader(tempFiles[pInputs]);
                RunReader b = new RunReader(tempFiles[pInputs + MERGE_SECOND_OFFSET]);
                DataOutputStream first = openWriter(tempFiles[pOutputs]);
                DataOutputStream second = openWriter(tempFiles[pOutputs + MERGE_SECOND_OFFSET])) {
            boolean toFirst = true;
            while (a.hasCurrent() || b.hasCurrent()) {
                long written = mergeRuns(a, b, pRunLength, toFirst ? first : second);
                if (!toFirst) {
                    writtenInSecond += written;
                }
                toFirst = !toFirst;
            }
        }
        return writtenInSecond;
    }

    /** Mezcla un tramo de cada entrada; solo compara los 2 registros actuales. */
    private static long mergeRuns(RunReader pA, RunReader pB, long pRunLength,
            DataOutputStream pOut) throws IOException {
        long takenA = EMPTY_COUNT;
        long takenB = EMPTY_COUNT;
        while (takenA < pRunLength && pA.hasCurrent() && takenB < pRunLength && pB.hasCurrent()) {
            if (pA.current().getName().compareTo(pB.current().getName()) <= SAME_NAME) {
                writeEntry(pOut, pA.current());
                pA.advance();
                takenA++;
            } else {
                writeEntry(pOut, pB.current());
                pB.advance();
                takenB++;
            }
        }
        while (takenA < pRunLength && pA.hasCurrent()) {
            writeEntry(pOut, pA.current());
            pA.advance();
            takenA++;
        }
        while (takenB < pRunLength && pB.hasCurrent()) {
            writeEntry(pOut, pB.current());
            pB.advance();
            takenB++;
        }
        return takenA + takenB;
    }

    // ------------------------------------------------------------ auxiliares de archivos

    private void append(IndexEntry pEntry) throws StorageException {
        File parent = indexFile.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs(); // si no se pudo crear, falla el open de abajo
        }
        try (RandomAccessFile file = new RandomAccessFile(indexFile, FILE_MODE_READ_WRITE)) {
            file.seek(file.length());
            file.write(RecordingStorage.encodeName(pEntry.getName()));
            file.writeLong(pEntry.getHeaderPosition());
        } catch (IOException e) {
            throw new StorageException(ERR_INDEX_WRITE + indexFile.getPath(), e);
        }
    }

    private void truncate(long pLength) throws StorageException {
        try (RandomAccessFile file = new RandomAccessFile(indexFile, FILE_MODE_READ_WRITE)) {
            file.setLength(pLength);
        } catch (IOException e) {
            throw new StorageException(ERR_INDEX_WRITE + indexFile.getPath(), e);
        }
    }

    private void deleteTempFiles() {
        for (File temp : tempFiles) {
            if (!temp.isFile()) {
                continue; // nunca se creó (por ejemplo, la carpeta no sirve)
            }
            try {
                Files.delete(temp.toPath());
            } catch (IOException e) {
                System.err.println(ERR_TEMP_DELETE + temp.getPath());
            }
        }
    }

    private static IndexEntry readAt(RandomAccessFile pFile, long pIndex) throws IOException {
        pFile.seek(pIndex * INDEX_RECORD_SIZE);
        byte[] name = new byte[NAME_LENGTH];
        pFile.readFully(name);
        return new IndexEntry(RecordingStorage.decodeName(name), pFile.readLong());
    }

    private static DataOutputStream openWriter(File pFile) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pFile)));
    }

    private static void writeEntry(DataOutputStream pOut, IndexEntry pEntry) throws IOException {
        pOut.write(RecordingStorage.encodeName(pEntry.getName()));
        pOut.writeLong(pEntry.getHeaderPosition());
    }

    /**
     * Lectura secuencial de un archivo de registros del índice. Guarda solo el
     * registro actual: es uno de los 2 registros en memoria durante la mezcla.
     */
    private static final class RunReader implements AutoCloseable {

        private final DataInputStream in;
        private long remaining;
        private IndexEntry current;

        RunReader(File pFile) throws IOException {
            remaining = pFile.length() / INDEX_RECORD_SIZE;
            in = new DataInputStream(new BufferedInputStream(new FileInputStream(pFile)));
            try {
                advance();
            } catch (IOException e) {
                in.close();
                throw e;
            }
        }

        boolean hasCurrent() {
            return current != null;
        }

        IndexEntry current() {
            return current;
        }

        void advance() throws IOException {
            if (remaining == EMPTY_COUNT) {
                current = null;
                return;
            }
            byte[] name = new byte[NAME_LENGTH];
            in.readFully(name);
            current = new IndexEntry(RecordingStorage.decodeName(name), in.readLong());
            remaining--;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }
}
