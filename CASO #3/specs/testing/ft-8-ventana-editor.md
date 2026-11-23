---
id: ft-8
layer: testing
status: draft
---

# Ventana del editor e integración

## General description

- Pruebas JUnit 5 de `EditorController`, sin abrir ventanas: pasan sin pantalla.
- Usan `Recorder`, `Player` y `RecordingLibrary` reales sobre archivos en `@TempDir`, comandos reales hacia el `OscReceiver` de prueba, instrumentos falsos y una vista falsa.
- La ventana Swing no tiene pruebas automáticas.

## Inputs

- Archivos de grabaciones en `@TempDir` (con `RecordingFiles` para grabaciones ya guardadas).
- `OscReceiver` + `OscClient` + los 3 comandos.
- `FakeEditorView` y `StubInstrument`.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `EditorControllerTest`
  - `startsIdle`.
  - `cannotRecordWhilePlaying`: con una grabación sonando, `startRecording` → `IllegalStateException`; `recordings.music` no crece.
  - `cannotPlayWhileRecording`: grabando, `play` → `IllegalStateException`; no llega nada al `OscReceiver` por la reproducción.
  - `cannotPlayWhileNaming`.
  - `cannotRecordTwice`.
  - `recordDuplicateNameThenOtherNameSaves`: con `"Cancion1"` ya guardada: grabar, tocar un evento, detener → `NAMING`; `saveRecording("Cancion1")` → `DuplicateRecordingNameException` y sigue `NAMING`; `saveRecording("Cancion2")` → `IDLE`, `recordingCount()` = 2, `recordingAt(1)` es `"Cancion2"` y la vista recibió `recordingsChanged()`.
  - `invalidNameKeepsNaming`: `"canción"` → `IllegalArgumentException`, sigue `NAMING`.
  - `cancelDiscardsRecording`: grabar, detener, `discardRecording` → `IDLE`; el largo de `recordings.music` es el de antes y `recordingCount()` no cambió.
  - `playsSavedRecordingAndReturnsToIdle`: grabación guardada con 2 comandos cortos; `play` → `PLAYING`; el receptor recibe los 2 mensajes; antes de un timeout el estado vuelve a `IDLE` y la vista recibió `stateChanged(IDLE)`.
  - `stopPlaybackReturnsToIdleQuickly`: grabación larga; `stopPlayback` deja `IDLE` en menos de `PLAYER_STOP_TIMEOUT_MS`.
  - `playUnknownNameThrowsAndStaysIdle` → `RecordingNotFoundException`.
  - `stateChangesAreReportedToView`: la secuencia grabar → detener → guardar produce `RECORDING`, `NAMING`, `IDLE` en la vista, en ese orden.
  - `failingInstrumentDoesNotStopOthers`: 3 instrumentos, el segundo falla al iniciar; los otros dos iniciaron y `getInstrumentStatus()` contiene las 3 etiquetas y el motivo del fallo.
  - `shutdownWhileRecordingIsOrdered`: grabando con un `noteOn` del órgano encendido; `shutdown` → la grabación se descartó (largo de antes), el `Player` no está sonando, los instrumentos iniciados recibieron `stop()` y el que falló no, y el receptor recibió `/n_set gate 0` para el nodo del órgano.
  - `shutdownWhilePlayingStopsPlayer`.
  - `shutdownTwiceDoesNothingTheSecondTime`.
  - `shutdownReportsFailingStepAndContinues`: un instrumento lanza en `stop()`; `showError` recibe el mensaje y los demás se detienen igual.
  - `formatEventShowsTimeSynthActionNoteAndDuration`: piano con 500 ms contiene `PIANO`, `TRIGGER`, `60` y `500 ms`; `organOn` muestra `MONITOR_NO_VALUE` en la duración.
  - `formatListEntryShowsNameAndDate`: la fecha sigue `LIST_DATE_PATTERN` en la zona del sistema.
- Ninguna prueba abre ventanas ni necesita `scsynth`, control, micrófono o webcam.

## Assets

- `src/test/powerband/editor/helpers/FakeEditorView.java`: `IEditorView` que guarda en listas seguras para hilos los estados recibidos, la cantidad de `recordingsChanged()` y los errores.
- `src/test/powerband/editor/helpers/StubInstrument.java`: `Instrument` con `setFailOnStart(boolean)`, `setFailOnStop(boolean)`, `isStarted()`, `isStopped()`.
- Reutiliza `src/test/powerband/editor/helpers/RecordingFiles.java`, `src/test/powerband/command/helpers/OscReceiver.java` y `src/test/powerband/instrument/helpers/FakeInstrument.java` (para tocar eventos por un comando real mientras se graba).

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; fuentes en `src/test/`.
- Archivo: `src/test/powerband/editor/EditorControllerTest.java`.
- Clases `<ClassName>Test`; métodos que describen comportamiento.
- Ninguna clase de `src/powerband/ui` se carga en las pruebas.
- Estados, timeouts, formatos y etiquetas desde `IConstants`.
- Las grabaciones que se reproducen tienen tiempos cortos (menos de 100 ms) salvo la de `stopPlaybackReturnsToIdleQuickly`.
- Las esperas usan un ciclo corto con timeout máximo, no un `sleep` fijo; cada prueba llama `shutdown()` en `finally` y cierra cliente y receptor con try-with-resources.
- Las pruebas de "no suena nada" leen el receptor con su timeout corto y esperan `null`.

## Business workflow

1. Crear archivos en `@TempDir`, `OscReceiver`, `OscClient`, los 3 comandos, `Recorder`, `Player`, `RecordingLibrary` y `EditorController` con una `FakeEditorView`.
2. Agregar `StubInstrument` y llamar `startInstruments` cuando la prueba lo necesite.
3. Ejecutar la secuencia de acciones.
4. Verificar estado, avisos de la vista, archivos, mensajes OSC recibidos y estado de los instrumentos.
5. `shutdown` en `finally`.
