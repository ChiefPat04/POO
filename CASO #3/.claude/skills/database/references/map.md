# Database

After you change this layer, update this file so the next run stays current.

Source: `src/powerband/storage/` (archivos binarios en `data/`, creados en ejecución)

## Implemented
- Lectura y escritura de `recordings.music` y `recordings.list` (ft-5): `src/powerband/storage/RecordingStorage.java`
- Tamaños, tipos y códigos del formato: `src/powerband/util/IConstants.java`
- `recordings.index` todavía no existe (ft-6).

## Schema
- `data/recordings.music` — registros de `RECORD_SIZE` (64) bytes, big-endian, relleno con ceros:
  - Encabezado: tipo `1` (byte) + nombre (32 ASCII, espacios) + inicio (long) + fin (long) + 15 de relleno. Inicio = posición del primer comando; fin = posición siguiente al último (`PENDING_POSITION` = -1 mientras no se cierra).
  - Comando: tipo `2` (byte) + tiempo relativo ms (long) + synth (byte: 1 piano, 2 bajo, 3 órgano) + acción (byte: 1 trigger, 2 noteOn, 3 noteOff) + nota, durMs, tercera (int) + amp, r1..r3, a1..a3 (float) + 13 de relleno.
- `data/recordings.list` — registros de `LIST_RECORD_SIZE` (40) bytes: nombre (32) + fecha en ms (long). Orden de archivo = orden cronológico.

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
