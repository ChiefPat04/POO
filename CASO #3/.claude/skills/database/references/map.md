# Database

After you change this layer, update this file so the next run stays current.

Source: `src/powerband/storage/` (archivos binarios en `data/`, creados en ejecución)

## Implemented
- Lectura y escritura de `recordings.music` y `recordings.list` (ft-5): `src/powerband/storage/RecordingStorage.java`
- Tamaños, tipos y códigos del formato: `src/powerband/util/IConstants.java`
- Índice por nombre en `recordings.index` con Balanced Merge Sort de 2 vías (4 temporales) y búsqueda binaria en disco (ft-6): `src/powerband/storage/RecordingIndex.java`
- Contrato que usa el grabador: `src/powerband/storage/IRecordingIndexer.java`

## Schema
- `data/recordings.music` — registros de `RECORD_SIZE` (64) bytes, big-endian, relleno con ceros:
  - Encabezado: tipo `1` (byte) + nombre (32 ASCII, espacios) + inicio (long) + fin (long) + 15 de relleno. Inicio = posición del primer comando; fin = posición siguiente al último (`PENDING_POSITION` = -1 mientras no se cierra).
  - Comando: tipo `2` (byte) + tiempo relativo ms (long) + synth (byte: 1 piano, 2 bajo, 3 órgano) + acción (byte: 1 trigger, 2 noteOn, 3 noteOff) + nota, durMs, tercera (int) + amp, r1..r3, a1..a3 (float) + 13 de relleno.
- `data/recordings.list` — registros de `LIST_RECORD_SIZE` (40) bytes: nombre (32) + fecha en ms (long). Orden de archivo = orden cronológico.
- `data/recordings.index` — registros de `INDEX_RECORD_SIZE` (40) bytes: nombre (32 ASCII, espacios) + posición del encabezado en `recordings.music` (long). Ordenado por nombre con `String.compareTo` (distingue mayúsculas).
- Temporales del ordenamiento: `merge1.tmp`..`merge4.tmp` (`MERGE_TEMP_FILES`) en la carpeta de temporales; se borran al terminar cada `add`.

## Classes
### RecordingStorage
Única clase que toca los dos archivos; abre y cierra el archivo en cada operación; métodos de archivo `synchronized`. `src/powerband/storage/RecordingStorage.java`
- `RecordingStorage()` — usa `MUSIC_FILE` y `LIST_FILE`
- `RecordingStorage(File pMusicFile, File pListFile)`
- `musicLength()` — largo de `recordings.music` (0 si no existe)
- `appendHeader(RecordingHeader pHeader)` — escribe al final, devuelve la posición (`throws StorageException`)
- `writeHeader(long pPosition, RecordingHeader pHeader)` — reescribe en sitio con `seek` (`throws StorageException`)
- `readHeader(long pPosition)` — `StorageException` si el registro no es encabezado
- `appendEvent(SynthEvent pEvent)` — escribe un comando al final; `timeMs` ya relativo (`throws StorageException`)
- `readEvent(long pPosition)` — reconstruye el `SynthEvent` con las fábricas; `StorageException` si no es comando
- `truncateMusic(long pLength)` — `setLength` (`throws StorageException`)
- `appendListEntry(ListEntry pEntry)` — escribe al final de la lista (`throws StorageException`)
- `listContainsName(String pName)` — búsqueda secuencial en disco, distingue mayúsculas; `false` si no hay archivo
- `listCount()` — cantidad de entradas
- `readListEntry(long pIndex)` — entrada número `pIndex` desde 0 (`throws StorageException`)
- `isValidName(String pName)` — estático; 1 a 32 ASCII imprimible, sin espacio en los extremos
- `encodeName(String pName)` — estático; 32 bytes ASCII rellenados con espacios (más de 32 → `IllegalArgumentException`)
- `decodeName(byte[] pBytes)` — estático; quita el relleno del final

### RecordingIndex
`IRecordingIndexer` real. `add` agrega al final y reordena todo el archivo: distribuye en tramos de 1 entre `T1`/`T2` y mezcla entre los pares (`T1`,`T2`) ↔ (`T3`,`T4`) duplicando el tramo hasta que queda uno; solo 2 registros en memoria (`RunReader` privado). Métodos `synchronized`. `src/powerband/storage/RecordingIndex.java`
- `RecordingIndex()` — usa `INDEX_FILE` y `DATA_DIR`
- `RecordingIndex(File pIndexFile, File pTempDir)` — null → `IllegalArgumentException`
- `add(String pName, long pHeaderPosition)` — agrega y ordena; si falla, recorta el índice a como estaba (`throws StorageException`)
- `find(String pName)` — búsqueda binaria en disco con `seek`; `Optional<IndexEntry>` (`throws StorageException`)
- `count()` — largo / 40; 0 si no existe
- `readEntry(long pIndex)` — un registro por posición lógica; fuera de rango → `IllegalArgumentException` (`throws StorageException`)

### IRecordingIndexer
Contrato del índice que llama `Recorder.save`. `src/powerband/storage/IRecordingIndexer.java`
- `add(String pName, long pHeaderPosition)` — (`throws StorageException`)
