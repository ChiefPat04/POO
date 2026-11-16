---
id: ft-5
layer: backend
status: approved
---

# Grabación de música

## General description

- `Recorder` (en `editor`) implementa `ISynthEventListener`, se registra en los 3 comandos y, mientras graba, escribe cada `SynthEvent` de inmediato en `recordings.music` con su tiempo relativo.
- Al detener apaga las notas encendidas del órgano (para que esos `noteOff` queden grabados) y espera un nombre válido y único para cerrar la grabación.
- Al guardar reescribe el encabezado, agrega la grabación a `recordings.list` y avisa al índice por `IRecordingIndexer`.
- Agrega `OrganCommand.allNotesOff()`, los modelos `RecordingHeader` y `ListEntry`, y las excepciones `StorageException` y `DuplicateRecordingNameException`.
- El formato binario y la clase que lee/escribe los archivos están en `specs/database/ft-5-grabacion-musica.md`.

## Inputs

- `SynthEvent` con `timeMs` absoluto, notificado por `PianoCommand`, `BassCommand` y `OrganCommand` desde los hilos de los instrumentos.
- Nombre de la grabación (`String`), que le pasa el editor (ft-8) a `save`.
- `RecordingStorage` e `IRecordingIndexer`, por constructor.

## Outputs

- Registros en `recordings.music` y `recordings.list` (por medio de `RecordingStorage`).
- Llamada `IRecordingIndexer.add(nombre, posiciónDelEncabezado)` al guardar.
- `StorageException`: falla de E/S al escribir o leer.
- `DuplicateRecordingNameException`: el nombre ya está en `recordings.list`.
- `IllegalArgumentException`: nombre inválido (el editor valida antes con `RecordingStorage.isValidName`).
- `IllegalStateException`: llamada fuera de estado (`start` grabando, `stop` sin grabar, `save`/`discard` sin grabación pendiente).

## Test criteria

- Solo se graban los eventos recibidos entre `start` y `stop`; antes y después se ignoran.
- El tiempo grabado es `evento.getTimeMs() - getStartTimeMs()`; si da negativo se graba `RECORDING_MIN_TIME_MS` (0).
- Tras `start` el encabezado provisional tiene nombre vacío (32 espacios), inicio = posición del encabezado + `RECORD_SIZE` y fin = `PENDING_POSITION`.
- `stop` con notas del órgano encendidas graba un `NOTE_OFF` por cada una, después de todos los demás eventos.
- `save` con nombre válido y único: encabezado reescrito con nombre, inicio y fin (fin = largo del archivo al detener); un registro nuevo al final de `recordings.list`; una llamada a `IRecordingIndexer.add` con el nombre y la posición del encabezado.
- `save` con nombre repetido lanza `DuplicateRecordingNameException`, no cambia ningún archivo, no llama al indexador y deja la grabación pendiente para reintentar con otro nombre.
- `"Cancion1"` y `"cancion1"` son nombres distintos.
- Nombre inválido (null, vacío, más de 32, con tilde/ñ u otro no ASCII, con espacio al inicio o al final) → `IllegalArgumentException`, grabación sigue pendiente.
- `discard` deja `recordings.music` con el largo que tenía antes de `start`.
- Una segunda grabación empieza donde terminó la primera; la primera no cambia.
- Varios hilos notificando a la vez: se graban todos los eventos, cada uno en un registro completo de `RECORD_SIZE`.
- Si una escritura falla dentro de `onSynthEvent`, `stop` lanza esa `StorageException`.
- `OrganCommand.allNotesOff()` envía `/n_set gate 0` a cada nodo encendido, notifica un `NOTE_OFF` por nota y deja el mapa vacío; sin notas encendidas no envía ni notifica nada.

## Assets

- `Recorder implements ISynthEventListener`:
  - `Recorder(RecordingStorage pStorage, IRecordingIndexer pIndexer, ISynthCommand pPiano, ISynthCommand pBass, OrganCommand pOrgan)` — se registra con `addListener` en los 3.
  - `void start() throws StorageException`
  - `void stop() throws StorageException, SynthConnectionException`
  - `void save(String pName) throws StorageException, DuplicateRecordingNameException`
  - `void discard() throws StorageException`
  - `void onSynthEvent(SynthEvent pEvent)`
  - `boolean isRecording()`, `boolean isPendingName()`, `long getStartTimeMs()`
- `IRecordingIndexer` (en `storage`): `void add(String pName, long pHeaderPosition) throws StorageException`. Solo la interfaz; la implementa ft-6.
- `OrganCommand`: `public void allNotesOff() throws SynthConnectionException`.
- Modelo (inmutables, con getters):
  - `RecordingHeader(String pName, long pStartPosition, long pEndPosition)`
  - `ListEntry(String pName, long pCreatedAtMs)`
- Excepciones:
  - `StorageException extends PowerBandException` (constructores mensaje y mensaje + causa).
  - `DuplicateRecordingNameException extends PowerBandException` (mensaje con el nombre rechazado).
- Claves nuevas en `IConstants`: `RECORDING_MIN_TIME_MS` (0), `ERR_ALREADY_RECORDING`, `ERR_NOT_RECORDING`, `ERR_NO_PENDING_RECORDING`, `ERR_DUPLICATE_NAME`, `ERR_INVALID_NAME`, `ERR_NULL_STORAGE`, `ERR_NULL_INDEXER`. Las del formato binario están en la spec de database.

## Implementation guidelines

- Archivos:
  - `src/powerband/editor/Recorder.java`
  - `src/powerband/storage/IRecordingIndexer.java`
  - `src/powerband/command/OrganCommand.java` (agregar `allNotesOff`)
  - `src/powerband/model/RecordingHeader.java`, `ListEntry.java`
  - `src/powerband/exception/StorageException.java`, `DuplicateRecordingNameException.java`
  - `src/powerband/util/IConstants.java`
- Estado del `Recorder` con un enum privado: `IDLE` → `RECORDING` → `PENDING_NAME` → `IDLE`.
- `start`, `stop`, `save`, `discard` y `onSynthEvent` son `synchronized` sobre el `Recorder`.
- `stop` llama `allNotesOff()` **con el lock tomado y todavía en `RECORDING`**: los `NOTE_OFF` vuelven por `onSynthEvent` en el mismo hilo (lock reentrante) y se graban. Después cambia a `PENDING_NAME` y guarda el largo del archivo como posición de fin.
- `allNotesOff`: copia las notas encendidas dentro de un bloque `synchronized` y luego llama `execute(SynthEvent.organOff(nota))` para cada una. No apaga los nodos directamente: tiene que pasar por `execute` para que se notifique a los listeners.
- `onSynthEvent` no puede lanzar excepciones checked: si `appendEvent` falla, guarda la primera `StorageException`, deja de escribir y `stop` la relanza (después de pasar a `PENDING_NAME`, para poder llamar `discard`).
- El tiempo relativo se pone con `pEvent.withTimeMs(...)` antes de pasarlo a `RecordingStorage`.
- `save` no pide el nombre: lo recibe. El diálogo y el reintento son del editor (ft-8).
- La unicidad se revisa con `RecordingStorage.listContainsName`, que lee `recordings.list` en disco registro por registro. No se guardan los nombres en memoria.
- Orden en `save`: validar nombre → revisar unicidad → reescribir encabezado → agregar a la lista → `IRecordingIndexer.add` → `IDLE`. Si `add` falla, la `StorageException` se propaga.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: implementación del índice (ft-6), cargar y reproducir (ft-7), botones y diálogo del nombre (ft-8), recuperar grabaciones sin cerrar tras un cierre inesperado (quedan con fin `PENDING_POSITION` y sin entrada en lista ni índice).
- Los comandos se guardan en orden de llegada. Dos eventos casi simultáneos de hilos distintos pueden quedar con tiempos invertidos por pocos ms; el reproductor (ft-7) debe tratar un intervalo negativo como 0.

## Business workflow

1. El editor crea `Recorder` con el storage, el indexador y los 3 comandos; el `Recorder` se registra como listener.
2. `start`: anota la posición final de `recordings.music`, escribe ahí el encabezado provisional y guarda `System.currentTimeMillis()` como hora de inicio.
3. Por cada evento en `RECORDING`: calcula el tiempo relativo y lo agrega al archivo.
4. `stop`: `allNotesOff()` del órgano (se graban los `NOTE_OFF`), guarda la posición de fin, pasa a `PENDING_NAME`.
5. `save(nombre)`: valida, revisa unicidad en `recordings.list`, reescribe el encabezado, agrega `ListEntry(nombre, ahora)` y llama `IRecordingIndexer.add(nombre, posiciónDelEncabezado)`.
6. Si el nombre está repetido, el editor pide otro y repite el paso 5. Si el usuario cancela, `discard` recorta el archivo a la posición del encabezado.
