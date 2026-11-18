---
id: ft-6
layer: database
status: approved 
---

# Índice: Balanced Merge Sort y búsqueda binaria

## General description

- `RecordingIndex` (en `storage`) implementa `IRecordingIndexer` (contrato de ft-5) y maneja `data/recordings.index`: registros de tamaño fijo con nombre + posición del encabezado en `recordings.music`, ordenados por nombre.
- `add` agrega el registro al final del índice y vuelve a ordenar todo el archivo con **Balanced Merge Sort de 2 vías y 4 archivos temporales**, tramos iniciales de 1 registro.
- `find` hace **búsqueda binaria en disco** con `RandomAccessFile`.
- Todo ocurre en disco: en memoria solo están los 2 registros que se comparan. Nunca se carga el índice a un arreglo ni a una colección.
- Agrega el modelo `IndexEntry`.

## Inputs

- `add(nombre, posiciónDelEncabezado)` desde `Recorder.save` (ft-5).
- `find(nombre)` desde la carga de una grabación (ft-7).
- Rutas del índice y de la carpeta de temporales por constructor (por defecto `IConstants.INDEX_FILE` y `IConstants.DATA_DIR`).

## Outputs

- `data/recordings.index` ordenado por nombre.
- `Optional<IndexEntry>` desde `find`.
- `StorageException` ante cualquier `IOException`; el índice queda como estaba antes de la llamada que falló.

## Test criteria

- Cada registro de `recordings.index` mide `INDEX_RECORD_SIZE` (40) bytes; el largo del archivo es `cantidad × 40`.
- Después de cada `add` el índice queda ordenado por nombre, sin perder ni duplicar registros.
- Funciona con 0, 1, 2, 3 y cantidades que no son potencia de 2 (5, 7, 13) y con nombres que llegan en orden, en orden inverso y desordenados.
- El orden distingue mayúsculas de minúsculas como `String.compareTo` (`"Cancion1"` < `"cancion1"`); ambos nombres pueden estar a la vez.
- `find` devuelve la posición correcta del primero, el del medio, el último y cada uno de muchos registros; un nombre que no está → `Optional.empty()`; índice vacío o inexistente → `Optional.empty()`.
- Al terminar `add` (bien o con error) no queda ningún archivo temporal en la carpeta.
- Los 4 temporales se usan en cada ordenamiento con 2 o más registros.
- Con `Recorder` real: después de `save`, `find(nombre)` da una posición donde `RecordingStorage.readHeader` lee ese mismo nombre.

## Assets

- Registro de `recordings.index` (`INDEX_RECORD_SIZE` = 40 bytes, `DataOutput`, big-endian): nombre (32 bytes ASCII rellenos con espacios, con `RecordingStorage.encodeName`) + posición del encabezado (`long`, 8).
- `IndexEntry` (en `model`, inmutable): `IndexEntry(String pName, long pHeaderPosition)`, `getName()`, `getHeaderPosition()`.
- `RecordingIndex implements IRecordingIndexer`:
  - `RecordingIndex(File pIndexFile, File pTempDir)`
  - `void add(String pName, long pHeaderPosition) throws StorageException` — agrega y ordena.
  - `Optional<IndexEntry> find(String pName) throws StorageException` — búsqueda binaria.
  - `long count() throws StorageException` — registros en el índice (largo / 40).
  - `IndexEntry readEntry(long pIndex) throws StorageException` — un registro por posición lógica (lo usan `find`, las pruebas y ft-7).
- Claves nuevas en `IConstants`:
  - Archivos: `INDEX_FILE` (`data/recordings.index`), `MERGE_TEMP_FILES` (`List.of("merge1.tmp", "merge2.tmp", "merge3.tmp", "merge4.tmp")`).
  - Tamaños: `INDEX_RECORD_SIZE` (40), `MERGE_INITIAL_RUN` (1), `MERGE_RUN_GROWTH` (2), `MERGE_WAYS` (2), `EMPTY_COUNT` (0), `SEARCH_HALF` (2).
  - Errores: `ERR_INDEX_SORT`, `ERR_INDEX_READ`, `ERR_INDEX_WRITE`, `ERR_NULL_INDEX_FILE`.

## Implementation guidelines

- Archivos:
  - `src/powerband/storage/RecordingIndex.java`
  - `src/powerband/model/IndexEntry.java`
  - `src/powerband/util/IConstants.java`
- Se reutilizan `RecordingStorage.encodeName` / `decodeName`; los nombres se comparan ya decodificados con `String.compareTo`.
- `add`:
  1. Escribe el registro nuevo al final de `recordings.index` (`RandomAccessFile`, modo `rw`; crea la carpeta si falta).
  2. Llama `sort()` (privado).
- `sort()` — Balanced Merge Sort de 2 vías con los 4 temporales `T1..T4` en `pTempDir`:
  1. Si hay menos de 2 registros, no hace nada.
  2. **Distribución:** lee el índice de forma secuencial y escribe los registros alternando entre `T1` y `T2`. Cada registro es un tramo de largo `MERGE_INITIAL_RUN` (1).
  3. **Pasadas de mezcla:** entradas = (`T1`, `T2`), salidas = (`T3`, `T4`), largo de tramo `L` = 1. Mientras queden tramos en las dos entradas: mezcla un tramo de cada entrada (hasta `L` registros de cada una, siempre escribiendo el menor de los 2 registros actuales) en la salida actual, y alterna la salida. Si una entrada se acaba, copia lo que queda de la otra.
  4. Al terminar la pasada, `L` se multiplica por `MERGE_RUN_GROWTH` y las salidas pasan a ser entradas (los roles de los pares se intercambian).
  5. Termina cuando una pasada deja todos los registros en una sola salida (la otra queda vacía).
  6. Copia esa salida sobre `recordings.index` (`Files.copy` con `REPLACE_EXISTING`) y borra los 4 temporales en un `finally`.
- Lectura de las entradas durante la mezcla: una clase anidada privada (`RunReader`) envuelve `DataInputStream` sobre `BufferedInputStream` y guarda solo el registro actual de esa entrada. Las dos instancias son los únicos 2 registros en memoria.
- Escritura de las salidas: `DataOutputStream` sobre `BufferedOutputStream`, un registro de 40 bytes a la vez.
- Si algo falla a mitad del ordenamiento, `recordings.index` no se toca hasta el paso 6, así que queda como antes; la `IOException` se envuelve en `StorageException` con la causa.
- `find`: búsqueda binaria clásica con `bajo = 0`, `alto = count() − 1`; en cada paso `seek(medio × INDEX_RECORD_SIZE)` y lee **un** registro con `RandomAccessFile` (modo `r`). No lee el archivo completo.
- `count`: largo del archivo / `INDEX_RECORD_SIZE`; archivo inexistente → 0.
- Todos los métodos públicos son `synchronized`.
- Todo stream y archivo con try-with-resources. Nunca `catch` vacío.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: reconstruir el índice desde `recordings.music`, borrar grabaciones, conectar `RecordingIndex` al `Recorder` en `PowerBandMain` (lo hace ft-8), cargar y reproducir (ft-7).

## Business workflow

1. `Recorder.save` llama `add(nombre, posición)`.
2. `add` escribe el registro al final del índice.
3. `sort` reparte los registros en `T1`/`T2` en tramos de 1.
4. Pasadas de mezcla entre los pares (`T1`,`T2`) ↔ (`T3`,`T4`), con tramos de 1, 2, 4, 8… hasta que queda un solo tramo.
5. El tramo final reemplaza a `recordings.index` y se borran los temporales.
6. `find(nombre)` hace búsqueda binaria en disco y devuelve la posición del encabezado.
