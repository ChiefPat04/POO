---
id: ft-5
layer: database
status: draft
---

# Grabación de música

## General description

- Define el formato binario de `data/recordings.music` y `data/recordings.list`.
- `RecordingStorage` (en `storage`) es la única clase que lee y escribe esos registros: encabezados, comandos y entradas de la lista.
- Todos los registros de `recordings.music` miden `RECORD_SIZE`; todos los de `recordings.list` miden `LIST_RECORD_SIZE`.

## Inputs

- `RecordingHeader`, `SynthEvent` (con `timeMs` ya relativo) y `ListEntry` desde `Recorder`.
- Rutas de los dos archivos por constructor (por defecto `IConstants.MUSIC_FILE` y `IConstants.LIST_FILE`).

## Outputs

- Bytes en `recordings.music` y `recordings.list`.
- `RecordingHeader`, `SynthEvent` y `ListEntry` leídos de disco (los usan las pruebas, ft-6 y ft-7).
- `StorageException` ante cualquier `IOException` o registro con tipo inesperado.

## Test criteria

- Todo registro escrito en `recordings.music` ocupa exactamente `RECORD_SIZE` bytes; el largo del archivo siempre es múltiplo de `RECORD_SIZE`.
- El primer byte es `RECORD_TYPE_HEADER` (1) o `RECORD_TYPE_COMMAND` (2).
- Encabezado, comando y entrada de lista se leen iguales a como se escribieron (todos los campos).
- Un nombre más corto que 32 se guarda con espacios hasta 32 bytes y se lee sin el relleno.
- `writeHeader` sobre una posición existente no cambia el largo del archivo ni los registros vecinos.
- `readHeader` sobre un registro de comando (y `readEvent` sobre un encabezado) lanza `StorageException`.
- `listContainsName` distingue mayúsculas de minúsculas y devuelve `false` si el archivo no existe.
- Las entradas de `recordings.list` se leen en el orden en que se agregaron.
- Si la carpeta `data/` no existe, se crea al escribir.

## Assets

- Registro de `recordings.music` (`RECORD_SIZE` = 64 bytes, formato de `DataOutput`, big-endian):

  | Encabezado | Bytes | | Comando | Bytes |
  |---|---|---|---|---|
  | tipo = 1 (`byte`) | 1 | | tipo = 2 (`byte`) | 1 |
  | nombre (ASCII, espacios) | 32 | | tiempo relativo ms (`long`) | 8 |
  | posición de inicio (`long`) | 8 | | synth (`byte`) | 1 |
  | posición de fin (`long`) | 8 | | acción (`byte`) | 1 |
  | relleno (ceros) | 15 | | nota (`int`) | 4 |
  | | | | durMs (`int`) | 4 |
  | | | | tercera (`int`) | 4 |
  | | | | amp (`float`) | 4 |
  | | | | r1, r2, r3 (`float`) | 12 |
  | | | | a1, a2, a3 (`float`) | 12 |
  | | | | relleno (ceros) | 13 |

  - Posición de inicio = posición del primer comando (encabezado + `RECORD_SIZE`). Posición de fin = posición siguiente al último comando.
- Registro de `recordings.list` (`LIST_RECORD_SIZE` = 40 bytes): nombre (32, ASCII, espacios) + fecha en ms (`long`, 8).
- `RecordingStorage`:
  - `RecordingStorage(File pMusicFile, File pListFile)`
  - `long musicLength() throws StorageException`
  - `long appendHeader(RecordingHeader pHeader) throws StorageException` — devuelve la posición donde quedó.
  - `void writeHeader(long pPosition, RecordingHeader pHeader) throws StorageException`
  - `RecordingHeader readHeader(long pPosition) throws StorageException`
  - `void appendEvent(SynthEvent pEvent) throws StorageException`
  - `SynthEvent readEvent(long pPosition) throws StorageException`
  - `void truncateMusic(long pLength) throws StorageException`
  - `void appendListEntry(ListEntry pEntry) throws StorageException`
  - `boolean listContainsName(String pName) throws StorageException`
  - `long listCount() throws StorageException`, `ListEntry readListEntry(long pIndex) throws StorageException`
  - `static boolean isValidName(String pName)`, `static byte[] encodeName(String pName)`, `static String decodeName(byte[] pBytes)`
- Claves nuevas en `IConstants`:
  - Archivos: `DATA_DIR` (`data`), `MUSIC_FILE` (`data/recordings.music`), `LIST_FILE` (`data/recordings.list`), `FILE_MODE_READ` (`r`), `FILE_MODE_READ_WRITE` (`rw`).
  - Tamaños: `RECORD_SIZE` (64), `NAME_LENGTH` (32), `LIST_RECORD_SIZE` (40), `HEADER_PADDING` (15), `COMMAND_PADDING` (13), `FILE_START` (0).
  - Tipos: `RECORD_TYPE_HEADER` (1), `RECORD_TYPE_COMMAND` (2).
  - Códigos: `SYNTH_CODE_PIANO` (1), `SYNTH_CODE_BASS` (2), `SYNTH_CODE_ORGAN` (3), `ACTION_CODE_TRIGGER` (1), `ACTION_CODE_NOTE_ON` (2), `ACTION_CODE_NOTE_OFF` (3).
  - Nombre: `NAME_PAD_CHAR` (`' '`), `NAME_MIN_CHAR` (32), `NAME_MAX_CHAR` (126), `PADDING_BYTE` (0), `PENDING_POSITION` (-1L).
  - Errores: `ERR_STORAGE_READ`, `ERR_STORAGE_WRITE`, `ERR_UNEXPECTED_RECORD_TYPE`, `ERR_UNKNOWN_SYNTH_CODE`, `ERR_UNKNOWN_ACTION_CODE`.

## Implementation guidelines

- Archivo: `src/powerband/storage/RecordingStorage.java`, más las constantes en `src/powerband/util/IConstants.java`.
- Cada registro se arma primero en memoria con `DataOutputStream` sobre un `ByteArrayOutputStream` (así mide exactamente RECORD_SIZE) y luego se escribe de una vez con `RandomAccessFile`. Para leer un registro se usa `DataInputStream` sobre un `ByteArrayInputStream`.
- `listContainsName` y la lectura de `recordings.list` usan `DataInputStream` sobre `BufferedInputStream` sobre `FileInputStream`, porque es lectura secuencial.
- `RandomAccessFile` solo se usa donde hay que saltar a una posición (`seek`, `setLength`).
- Cada operación abre el archivo, hace su trabajo y lo cierra con try-with-resources. Así cada comando queda en disco de inmediato.
- Todos los métodos que tocan archivos son `synchronized` (llegan eventos de varios hilos).
- Cada registro se arma completo y se escribe con su relleno, para que nunca quede un registro a medias.
- Nombre: `encodeName` rellena con `NAME_PAD_CHAR` hasta `NAME_LENGTH` y codifica en `StandardCharsets.US_ASCII`; `decodeName` quita el relleno del final.
- `isValidName`: no null, largo entre 1 y `NAME_LENGTH`, todos los caracteres entre `NAME_MIN_CHAR` y `NAME_MAX_CHAR`, sin espacio al inicio ni al final (se confundiría con el relleno).
- Synth y acción se guardan con los códigos de `IConstants`, no con `ordinal()`.
- `readEvent` reconstruye el `SynthEvent` con las fábricas (`piano`, `bass`, `organOn`, `organOff`) y `withTimeMs`; el constructor de `SynthEvent` sigue `private`.
- `listContainsName` recorre `recordings.list` en disco, un registro a la vez, comparando el nombre; se detiene en la primera coincidencia. No carga la lista a un arreglo ni colección.
- `IOException` se envuelve en `StorageException` con la causa; nunca `catch` vacío.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: `recordings.index` e `IndexEntry` (ft-6).

## Business workflow

1. `appendHeader`: va al final de `recordings.music`, escribe tipo 1 + nombre + inicio + fin + relleno, devuelve la posición.
2. `appendEvent`: va al final, escribe tipo 2 + campos del evento + relleno.
3. `writeHeader`: `seek` a la posición del encabezado y lo reescribe completo.
4. `listContainsName`: lee `recordings.list` de inicio a fin, registro por registro.
5. `appendListEntry`: va al final de `recordings.list`, escribe nombre + fecha.
6. `truncateMusic`: `setLength` para descartar una grabación sin nombre.
