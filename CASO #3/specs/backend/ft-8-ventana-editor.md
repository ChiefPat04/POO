---
id: ft-8
layer: backend
status: draft
---

# Ventana del editor e integración

## General description

- `EditorController` (en `editor`, sin Swing) coordina `Recorder`, `Player`, `RecordingLibrary` y los instrumentos, y aplica las reglas de estado. La ventana (spec de frontend) solo muestra y llama al controlador.
- El controlador avisa a la vista por `IEditorView`; la ventana es la implementación real y las pruebas usan una falsa.
- `PowerBandMain` deja de ser provisional: arma todo, inicia los instrumentos, abre la ventana y hace el cierre ordenado al cerrarla.

## Inputs

- Acciones de la ventana: grabar, detener grabación, guardar con un nombre, descartar, cargar un nombre, reproducir lo cargado, detener reproducción, cerrar.
- Aviso de `Player` por `IPlaybackListener.onPlaybackFinished`, desde el hilo del reproductor.
- Instrumentos con su etiqueta, por `addInstrument`.

## Outputs

- Llamadas a `Recorder`, `Player`, `RecordingLibrary`, `Instrument.start/stop` y `OrganCommand.allNotesOff`.
- Avisos a `IEditorView`: cambio de estado, lista de grabaciones cambiada, error.
- Texto de estado de los instrumentos y una línea de monitor por cada `SynthEvent`.
- `PowerBandException` (y subclases) hacia la ventana cuando una acción falla; `IllegalStateException` si la acción no corresponde al estado.

## Test criteria

- Estado inicial `IDLE`.
- `startRecording` en `IDLE` → `RECORDING`. En `PLAYING`, `RECORDING` o `NAMING` → `IllegalStateException` y no se escribe nada.
- `load` en `IDLE` busca el nombre con `RecordingLibrary.load` (búsqueda binaria en el índice) y guarda su encabezado como grabación cargada; `getLoadedName()` devuelve ese nombre. En `RECORDING` o `NAMING` → `IllegalStateException`.
- `play` en `IDLE` con una grabación cargada → `PLAYING`. Sin grabación cargada, o en `RECORDING` o `NAMING` → `IllegalStateException` y no suena nada.
- `stopRecording` → `NAMING`, también cuando `Recorder.stop` lanza (el grabador quedó pendiente de nombre).
- `saveRecording` con nombre repetido → `DuplicateRecordingNameException`, sigue en `NAMING`; con otro nombre → `IDLE` y `recordingsChanged()` a la vista.
- `saveRecording` con nombre inválido → `IllegalArgumentException`, sigue en `NAMING`.
- `discardRecording` → `IDLE`; `recordings.music` vuelve al largo de antes de grabar y la lista no cambia.
- `load` y luego `play` de un nombre guardado → `PLAYING`; sus comandos llegan a los comandos de synth; al terminar sola, `onPlaybackFinished` deja `IDLE` y avisa a la vista. La grabación sigue cargada para volver a tocarla.
- `load` de un nombre que no existe → `RecordingNotFoundException`, sigue en `IDLE`; la grabación cargada antes no cambia.
- `stopPlayback` → `IDLE` en menos de `PLAYER_STOP_TIMEOUT_MS`.
- Cada cambio de estado llama `stateChanged` una vez, con el estado nuevo.
- `startInstruments` inicia todos; si uno lanza `PowerBandException`, sigue con los demás; `getInstrumentStatus()` nombra los que iniciaron y los que fallaron con su motivo.
- `shutdown` en `RECORDING`: detiene y descarta la grabación, detiene el `Player`, detiene solo los instrumentos que iniciaron, llama `allNotesOff`. No lanza aunque un paso falle (el error va a `showError`) y sigue con los pasos siguientes. Llamarlo dos veces no hace nada la segunda vez.
- `formatEvent` produce la línea de monitor con hora, synth, acción, nota y duración (o `MONITOR_NO_VALUE` si no aplica).
- `formatListEntry` produce `nombre  fecha` con `LIST_DATE_PATTERN`.

## Assets

- `EditorState` (enum, `src/powerband/editor/EditorState.java`): `IDLE`, `RECORDING`, `NAMING` (detenida, esperando nombre), `PLAYING`.
- `IEditorView` (`src/powerband/editor/IEditorView.java`). Se puede llamar desde cualquier hilo; la implementación se encarga de pasar al hilo de Swing:
  - `void stateChanged(EditorState pState)`
  - `void recordingsChanged()`
  - `void showError(String pMessage)`
- `EditorController implements IPlaybackListener` (`src/powerband/editor/EditorController.java`):
  - `EditorController(Recorder pRecorder, Player pPlayer, RecordingLibrary pLibrary, OrganCommand pOrgan)` — null → `IllegalArgumentException`; se registra con `pPlayer.setListener(this)`.
  - `void setView(IEditorView pView)`
  - `void addInstrument(String pLabel, Instrument pInstrument)`
  - `void startInstruments()` — no lanza.
  - `String getInstrumentStatus()`
  - `EditorState getState()`
  - `void startRecording() throws StorageException`
  - `void stopRecording() throws StorageException, SynthConnectionException`
  - `void saveRecording(String pName) throws StorageException, DuplicateRecordingNameException`
  - `void discardRecording() throws StorageException`
  - `void load(String pName) throws StorageException, RecordingNotFoundException`
  - `String getLoadedName()` — `null` si no hay ninguna cargada.
  - `void play() throws StorageException`
  - `void stopPlayback()`
  - `void shutdown()` — no lanza.
  - `long recordingCount() throws StorageException`, `ListEntry recordingAt(long pIndex) throws StorageException` — delegan en `RecordingLibrary`, una entrada a la vez.
  - `static boolean isValidName(String pName)` — delega en `RecordingStorage.isValidName`.
  - `static String formatEvent(SynthEvent pEvent)`, `static String formatListEntry(ListEntry pEntry)`
  - `void onPlaybackFinished(String pName)`
- Claves nuevas en `IConstants`:
  - Etiquetas: `LABEL_GAMEPAD` (`Control PS5 → bajo`), `LABEL_MICROPHONE` (`Micrófono → órgano`), `LABEL_WEBCAM` (`Webcam → piano`).
  - Estado: `STATUS_STARTED` (`Iniciados: `), `STATUS_FAILED` (`Fallaron: `), `STATUS_NONE` (`ninguno`), `STATUS_SEPARATOR` (`, `), `STATUS_REASON_OPEN` (` (`), `STATUS_REASON_CLOSE` (`)`), `STATUS_PART_SEPARATOR` (`  |  `).
  - Monitor: `MONITOR_TIME_PATTERN` (`HH:mm:ss.SSS`), `MONITOR_LINE_FORMAT` (`%s  %-5s  %-8s  nota %3d  dur %s`), `MONITOR_NO_VALUE` (`-`), `MONITOR_DUR_SUFFIX` (` ms`).
  - Lista: `LIST_DATE_PATTERN` (`yyyy-MM-dd HH:mm:ss`), `LIST_LINE_FORMAT` (`%-32s  %s`).
  - Errores: `ERR_INVALID_STATE` (`Acción no permitida en el estado: `), `ERR_NULL_EDITOR_DEPENDENCY`, `ERR_SHUTDOWN_STEP` (`Error al cerrar: `), `ERR_SC_CONNECT` (`No se pudo conectar con SuperCollider: `).

## Implementation guidelines

- Archivos:
  - `src/powerband/editor/EditorController.java`, `EditorState.java`, `IEditorView.java`
  - `src/powerband/PowerBandMain.java`
  - `src/powerband/util/IConstants.java`
- `EditorController` no importa nada de `javax.swing` ni `java.awt`: así las pruebas corren sin pantalla.
- Estado en un campo `private EditorState state`; todos los métodos que lo leen o cambian son `synchronized`, porque `onPlaybackFinished` llega desde el hilo del `Player`.
- **`stopPlayback` no puede tener el lock del controlador mientras llama `player.stop()`**: `stop()` espera al hilo del reproductor, y ese hilo llama `onPlaybackFinished`, que necesita el mismo lock. Con el lock tomado, `stop()` se quedaría esperando hasta su timeout. Leer el estado con el lock, soltarlo y luego llamar `player.stop()`. Lo mismo en `shutdown`.
- `onPlaybackFinished` cambia a `IDLE` solo si el estado actual es `PLAYING`.
- Las llamadas a la vista se hacen después de cambiar el estado y fuera de la sección crítica cuando se pueda. Si la vista es `null`, se omiten.
- `stopRecording`: llama `recorder.stop()` dentro de `try/finally`; en el `finally` pasa a `NAMING` si `recorder.isPendingName()`. Así la grabación se puede guardar o descartar aunque una escritura haya fallado.
- `saveRecording`: después de `recorder.save`, si `recorder.isPendingName()` sigue en `NAMING`; si no, pasa a `IDLE` y llama `recordingsChanged()` (también si el indexador falló después de guardar en la lista, porque ya aparece).
- `load`: `library.load(nombre)` y guarda el `RecordingHeader`. `play`: `player.start(headerCargado)`; pasa a `PLAYING` antes de `start` y vuelve a `IDLE` si `start` lanza.
- `startInstruments` guarda, por etiqueta, los que iniciaron (para detenerlos) y los que fallaron con su mensaje (para el estado). No usa `System.err`.
- `shutdown`, en este orden, cada paso en su propio `try` y con un `catch` que llama `showError(ERR_SHUTDOWN_STEP + mensaje)`:
  1. `RECORDING` → `recorder.stop()`; `RECORDING` o `NAMING` → `recorder.discard()`.
  2. `player.stop()`.
  3. `stop()` de cada instrumento iniciado.
  4. `organ.allNotesOff()`.
  5. Marca que ya cerró; un segundo `shutdown` no hace nada.
- `OscClient` no es del controlador: lo cierra `PowerBandMain`.
- La lista no se copia a una colección: la ventana pide `recordingCount()` y `recordingAt(i)`, que leen de disco cada vez.
- Formatos: `DateTimeFormatter.ofPattern(...)` con `ZoneId.systemDefault()`; el texto del synth y la acción es `name()` del enum.
- `PowerBandMain`:
  1. `OscClient(SC_HOST, SC_PORT)` en try-with-resources. Si lanza `SynthConnectionException`, muestra `ERR_SC_CONNECT` en un `JOptionPane` y termina.
  2. Crea los 3 comandos, `RecordingStorage()`, `RecordingIndex()`, `Recorder(storage, index, piano, bass, organ)`, `Player(storage, piano, bass, organ)`, `RecordingLibrary(storage, index)` y el controlador.
  3. `addInstrument` con `GamepadInstrument(bass, new JamepadReader())`, `MicrophoneInstrument(organ, new MicrophoneSource())` y `WebcamInstrument(piano, new WebcamSource())`, con sus etiquetas.
  4. `startInstruments()`.
  5. Crea y muestra la ventana con `SwingUtilities.invokeLater`; le pasa los 3 comandos para el monitor y un `CountDownLatch`.
  6. Espera el latch (la ventana lo baja al cerrarse). Al salir del try se cierra `OscClient`.
- Así `OscClient` sigue cerrándose con try-with-resources aunque la ventana viva en otro hilo.
- Los errores de los hilos de fondo (instrumentos y `Player`) siguen yendo a `System.err`; llevarlos a un diálogo cambiaría clases de ft-2, ft-3, ft-4 y ft-7, y queda fuera de alcance. UDP no detecta un `scsynth` apagado (ver ft-1): solo hay diálogo cuando falla algo local.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: borrar o renombrar grabaciones, pausar, buscar por texto, elegir dispositivos, reiniciar un instrumento que falló.

## Business workflow

1. `PowerBandMain` arma todo, inicia los instrumentos y abre la ventana.
2. Grabar: `IDLE` → `startRecording` → `RECORDING`.
3. Detener: `stopRecording` → `NAMING`.
4. Nombre: `saveRecording` → `IDLE` y lista actualizada; si el nombre falla, la ventana pide otro; si cancela, `discardRecording` → `IDLE`.
5. Cargar y reproducir: `IDLE` → `load(nombre)` (búsqueda binaria) → `play()` → `PLAYING` → al terminar o con `stopPlayback`, `IDLE`.
6. Cerrar: `shutdown`, la ventana baja el latch y `PowerBandMain` cierra `OscClient`.
