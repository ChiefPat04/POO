---
id: ft-7
layer: backend
status: approved 
---

# Cargar y reproducir

## General description

- `RecordingLibrary` (en `editor`) da la lista cronológica de grabaciones desde `recordings.list` y carga una por nombre: búsqueda binaria en `recordings.index` (ft-6) y lectura del encabezado en `recordings.music` (ft-5).
- `Player` (en `editor`) reproduce una grabación: lee sus comandos de disco uno por uno y los envía con los mismos `ISynthCommand` que usan los instrumentos (patrón Bridge), respetando los intervalos grabados para que suene igual.
- La reproducción corre en su propio hilo y se puede detener; al detenerla no quedan notas del órgano encendidas.
- Agrega `RecordingNotFoundException` y la interfaz `IPlaybackListener` para avisar cuando termina una reproducción (la usará la ventana de ft-8).

## Inputs

- `RecordingStorage` (ft-5) y `RecordingIndex` (ft-6), por constructor de `RecordingLibrary`.
- `RecordingStorage` y los 3 `ISynthCommand` (piano, bajo, órgano), por constructor de `Player`.
- El nombre elegido por el usuario (ft-8 lo toma de la lista).

## Outputs

- `listCount()` / `listEntry(i)`: entradas de `recordings.list` en orden cronológico.
- `load(nombre)`: el `RecordingHeader` de esa grabación.
- Los `SynthEvent` de la grabación enviados a su comando, con las mismas distancias de tiempo que se grabaron.
- `RecordingNotFoundException` si el nombre no está en el índice; `StorageException` ante errores de disco.
- `IllegalStateException` si se llama `start` con una reproducción en curso.

## Test criteria

- `RecordingLibrary`:
  - `listCount` y `listEntry` devuelven las grabaciones en el orden en que se guardaron.
  - `load` de un nombre guardado devuelve su encabezado (nombre, inicio y fin correctos).
  - `load` de un nombre que no existe lanza `RecordingNotFoundException`; distingue mayúsculas.
- `Player`:
  - Cada comando de la grabación llega al comando de su synth (piano → piano, bajo → bajo, órgano → órgano), en el mismo orden y con los mismos parámetros.
  - Los comandos de otras grabaciones del mismo archivo no se envían.
  - El intervalo entre dos comandos enviados es el grabado, con una tolerancia de `PLAYER_TIMING_TOLERANCE_MS`; no depende de cuánto tiempo pasó desde que se cargó.
  - Una grabación vacía (inicio = fin) termina sin enviar nada.
  - `start` corre en otro hilo y vuelve de inmediato; al terminar, `isPlaying()` es `false` y el `IPlaybackListener` recibe `onPlaybackFinished` una vez.
  - `stop` a mitad de la reproducción deja de enviar comandos, envía `organOff` de cada nota del órgano que quedó encendida y avisa `onPlaybackFinished`.
  - `start` con una reproducción en curso lanza `IllegalStateException`; `stop` sin reproducción no falla.
  - Si un comando falla con `SynthConnectionException`, se reporta en `System.err` y la reproducción sigue con el siguiente.

## Assets

- `RecordingLibrary`:
  - `RecordingLibrary(RecordingStorage pStorage, RecordingIndex pIndex)`
  - `long listCount() throws StorageException`
  - `ListEntry listEntry(long pIndex) throws StorageException`
  - `RecordingHeader load(String pName) throws StorageException, RecordingNotFoundException`
- `Player`:
  - `Player(RecordingStorage pStorage, ISynthCommand pPiano, ISynthCommand pBass, ISynthCommand pOrgan)`
  - `void play(RecordingHeader pHeader) throws StorageException, InterruptedException` — reproduce en el hilo que llama (lo usan el hilo propio y las pruebas).
  - `void start(RecordingHeader pHeader)` — reproduce en un hilo daemon propio.
  - `void stop()`
  - `boolean isPlaying()`
  - `void setListener(IPlaybackListener pListener)`
- `IPlaybackListener` (en `editor`): `void onPlaybackFinished(String pName)`.
- `RecordingNotFoundException extends PowerBandException` (en `exception`): constructor con el nombre.
- Claves nuevas en `IConstants`:
  - `PLAYER_THREAD_NAME`, `PLAYER_STOP_TIMEOUT_MS` (1000), `PLAYER_NO_WAIT_MS` (0).
  - `PLAYER_TIMING_TOLERANCE_MS` (30) — solo lo usan las pruebas, pero va en `IConstants` como todo valor.
  - Mensajes: `ERR_RECORDING_NOT_FOUND`, `ERR_ALREADY_PLAYING`, `ERR_PLAYER_SEND`, `ERR_PLAYER_READ`, `ERR_NULL_HEADER`, `ERR_NULL_LIBRARY_DEPENDENCY`.

## Implementation guidelines

- Archivos:
  - `src/powerband/editor/RecordingLibrary.java`, `Player.java`, `IPlaybackListener.java`
  - `src/powerband/exception/RecordingNotFoundException.java`
  - `src/powerband/util/IConstants.java`
- `RecordingLibrary` no copia la lista a una colección: `listCount` y `listEntry` delegan en `RecordingStorage.listCount` / `readListEntry`, un registro a la vez. La ventana (ft-8) los pide uno por uno para llenar su lista.
- `load`: `index.find(nombre)`; vacío → `RecordingNotFoundException`; si no, `storage.readHeader(posición)`.
- `Player.play`:
  1. Toma la hora de inicio `t0 = System.currentTimeMillis()`.
  2. Recorre `recordings.music` desde `header.getStartPosition()` hasta `header.getEndPosition()`, de `RECORD_SIZE` en `RECORD_SIZE`, leyendo **un** comando por vez con `storage.readEvent`. No carga la grabación a una lista.
  3. Para cada comando espera hasta `t0 + timeMs` (`Thread.sleep` de la diferencia; si ya pasó, no espera) y lo envía con `execute` al comando de su synth. Esperar contra `t0` (y no contra el comando anterior) evita que los retrasos se acumulen.
  4. Lleva las notas del órgano encendidas (`NOTE_ON` agrega, `NOTE_OFF` quita) para poder apagarlas si se detiene.
- El comando de cada synth se elige por `SynthEvent.getSynth()` con un `switch` sobre `SynthType`; no hay `instanceof`.
- Hilo: daemon, con nombre `PLAYER_THREAD_NAME`. `stop()`: interrumpe, `join(PLAYER_STOP_TIMEOUT_MS)`. Al salir de `play` (normal o interrumpido) el hilo envía `organOff` de las notas que quedaron encendidas y llama `onPlaybackFinished`.
- `start` y `stop` usan un lock propio; `isPlaying` lee una bandera `volatile`.
- Los comandos notifican a sus listeners como siempre, así que el monitor del editor (ft-8) ve lo que se reproduce. Que el grabador grabe o no mientras se reproduce lo decide la ventana en ft-8.
- `SynthConnectionException` dentro del hilo se reporta con `ERR_PLAYER_SEND` y sigue; `StorageException` corta la reproducción y se reporta con `ERR_PLAYER_READ`. Nunca `catch` vacío.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: pausa y reanudar, velocidad, reproducir varias a la vez, la ventana y los botones (ft-8).

## Business workflow

1. La ventana pide `listCount` y `listEntry(i)` para mostrar las grabaciones en orden cronológico.
2. El usuario elige un nombre; `load(nombre)` hace la búsqueda binaria en el índice y lee el encabezado.
3. `Player.start(encabezado)` abre su hilo y reproduce los comandos en tiempo y forma.
4. Al terminar o al detener, se apagan las notas del órgano que quedaron encendidas y se avisa al listener.
