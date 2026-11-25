# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/powerband/` (JUnit 5, `mvn test`)

## Implemented
- Pruebas de ft-1 sin SuperCollider: un `DatagramSocket` local en puerto efímero hace de scsynth.
- Pruebas de ft-8 solo de `EditorController`, con `Recorder`/`Player`/`RecordingLibrary` reales sobre `@TempDir`, comandos hacia `OscReceiver`, vista e instrumentos falsos; no cargan `src/powerband/ui` y pasan con `-Djava.awt.headless=true`. La ventana se prueba a mano.
- Pruebas de ft-5 (grabación) sobre archivos en `@TempDir`; nunca tocan `data/`.
- Pruebas de ft-7 (cargar y reproducir) sobre `@TempDir` con comandos falsos que anotan la hora de llegada; intervalos con tolerancia `PLAYER_TIMING_TOLERANCE_MS`.
- Pruebas de ft-6 (índice) sobre `@TempDir`: orden después de cada `add`, búsqueda binaria, limpieza de temporales e integración con `Recorder`.
- Pruebas de ft-2 (micrófono) con señales sintéticas y una fuente de audio falsa. `MicrophoneSource` se prueba a mano con `mvn compile exec:java`.
- Pruebas de ft-3 (control PS5) con un lector falso; no cargan Jamepad. `JamepadReader` se prueba a mano con `mvn compile exec:java`.
- Pruebas de ft-4 (webcam) con cuadros generados y una fuente de cuadros falsa; no cargan Webcam Capture. `WebcamSource` se prueba a mano con `mvn compile exec:java`.

## Test classes
- `src/test/powerband/sc/OscClientTest.java` — formato OSC, nodeIds únicos con hilos, puerto inválido, host `.invalid`
- `src/test/powerband/model/SynthEventTest.java` — fábricas, valores por defecto, `withTimeMs`
- `src/test/powerband/command/PianoCommandTest.java` — mensaje `classicPiano`, rechazo de otros synths
- `src/test/powerband/command/BassCommandTest.java` — mensaje `stringBass`, rechazo de otros synths
- `src/test/powerband/command/OrganCommandTest.java` — noteOn/noteOff, reinicio, `midiToFreq`, concurrencia, `allNotesOff`
- `src/test/powerband/storage/RecordingStorageTest.java` — formato binario, tamaño fijo, relleno del nombre, reescritura del encabezado, lista, `isValidName`
- `src/test/powerband/storage/RecordingIndexTest.java` — tamaño fijo, orden tras cada `add` (en orden, inverso, desordenado, 3/5/7/13), mayúsculas, búsqueda del primero/medio/último y de 100 nombres, ausentes, nombre de 32, sin temporales, falla deja el índice igual
- `src/test/powerband/storage/RecordingIndexRecorderTest.java` — integración: lo que guarda `Recorder` se encuentra con `find` y apunta a su encabezado
- `src/test/powerband/editor/RecordingLibraryTest.java` — orden cronológico, carga por nombre, nombre inexistente, mayúsculas
- `src/test/powerband/editor/PlayerTest.java` — synth destino y orden, solo la grabación elegida, intervalos, independiente del momento de carga, grabación vacía, hilo y aviso, `stop` apaga el órgano, doble `start`, falla de envío
- `src/test/powerband/editor/EditorControllerTest.java` — reglas de estado, nombre repetido → otro nombre, cancelar descarta, cargar y reproducir, detener rápido, instrumentos que fallan, cierre ordenado, formatos
- `src/test/powerband/editor/RecorderTest.java` — flujo start/stop/save/discard, tiempo relativo, noteOff al detener, nombre duplicado o inválido, concurrencia, falla de escritura
- `src/test/powerband/command/SynthCommandListenerTest.java` — notificación y remoción de listeners
- `src/test/powerband/instrument/InstrumentTest.java` — delegación del Bridge y `setCommand`
- `src/test/powerband/instrument/PitchDetectorTest.java` — 440 Hz, cada semitono de 40 a 100, armónicos, silencio, ruido, RMS, redondeo a MIDI
- `src/test/powerband/instrument/MicrophoneInstrumentTest.java` — encendido tras ventanas estables, cambio de nota, apagado por silencio, histéresis, amp, fuera de rango, una sola nota, ciclo de vida del hilo
- `src/test/powerband/instrument/HandTrackerTest.java` — primer cuadro, cuadros iguales, cambio mínimo, cambio de luz, centro del movimiento, altura, cantidad de movimiento, `reset`
- `src/test/powerband/instrument/WebcamInstrumentTest.java` — disparo por golpe, histéresis y rearme, altura → nota 48–72 en Do mayor, lado → tercera, amp, duración, ciclo de vida del hilo y fuente sin cuadros, tiempo mínimo entre golpes, borde en `CAM_SPLIT_X`
- `src/test/powerband/instrument/GamepadInstrumentTest.java` — botones → notas, flanco, stick con histéresis, octavas, límites 28–55, duración por R2, ciclo de vida del hilo y reconexión

## Helpers
### OscReceiver
Receptor UDP que decodifica OSC. `src/test/powerband/command/helpers/OscReceiver.java`
- `getPort()` — puerto efímero
- `receive()` — un mensaje o `null` tras 300 ms
- `receiveAll()` — mensajes hasta un timeout sin datos

### OscMessage
Path + args decodificados. `src/test/powerband/command/helpers/OscMessage.java`
- `getPath()`, `getArgs()`, `arg(int pIndex)`

### FakeInstrument
`Instrument` sin dispositivo. `src/test/powerband/instrument/helpers/FakeInstrument.java`
- `playEvent(SynthEvent pEvent)` — llama `play`

### FakeEditorView
`IEditorView` que guarda avisos (listas seguras para hilos). `src/test/powerband/editor/helpers/FakeEditorView.java`
- `getStates()`, `getErrors()`, `getRecordingsChanged()`

### StubInstrument
`Instrument` que puede fallar al iniciar o detenerse. `src/test/powerband/editor/helpers/StubInstrument.java`
- `setFailOnStart(boolean pFail)`, `setFailOnStop(boolean pFail)`, `isStarted()`, `isStopped()`; constantes `START_FAILURE`, `STOP_FAILURE`

### FakeIndexer
`IRecordingIndexer` que guarda las llamadas. `src/test/powerband/editor/helpers/FakeIndexer.java`
- `add(String pName, long pHeaderPosition)`
- `getNames()`, `getPositions()`

### TestSignals
Bloques sintéticos de `AUDIO_BLOCK_SIZE`. `src/test/powerband/instrument/helpers/TestSignals.java`
- `sine(float pFrequency, float pAmplitude)`, `harmonics(float pFrequency, float pAmplitude)`, `silence()`, `noise(long pSeed, float pAmplitude)`, `noteFrequency(int pNote)` — estáticos

### FakeAudioSource
`IAudioSource` falso que repite un bloque. `src/test/powerband/instrument/helpers/FakeAudioSource.java`
- `setBlock(float[] pBlock)`, `setHasAudio(boolean pHasAudio)`, `setFailOnOpen(boolean pFail)`, `isClosed()`

### FakeGamepadReader
`IGamepadReader` falso. `src/test/powerband/instrument/helpers/FakeGamepadReader.java`
- `setState(GamepadState pState)`, `setFailOnOpen(boolean pFail)`, `getReadCount()`, `isClosed()`

### TimedCommand
`ISynthCommand` falso que anota cada evento con la hora de llegada. `src/test/powerband/editor/helpers/TimedCommand.java`
- `getEvents()`, `getTimes()`, `setFailing(boolean pFail)`

### RecordingFiles
Storage + índice reales en una carpeta. `src/test/powerband/editor/helpers/RecordingFiles.java`
- `RecordingFiles(File pDir)`
- `save(String pName, SynthEvent... pEvents)` — escribe encabezado, comandos, lista e índice; devuelve el `RecordingHeader`
- `getStorage()`, `getIndex()`, `library()`

### TestFrames
Cuadros 320×240 generados. `src/test/powerband/instrument/helpers/TestFrames.java`
- `blank()`, `filled()`, `withSquare(float pX, float pY, float pSize)`, `withPixel(int pX, int pY)` — estáticos

### FakeFrameSource
`IFrameSource` falso que entrega cuadros en ciclo. `src/test/powerband/instrument/helpers/FakeFrameSource.java`
- `setFrames(BufferedImage... pFrames)`, `setHasFrames(boolean pHasFrames)`, `setFailOnOpen(boolean pFail)`, `isClosed()`, `getEmptyReads()`

### RecordingCommand
`ISynthCommand` que guarda eventos (lista segura para hilos). `src/test/powerband/instrument/helpers/RecordingCommand.java`
- `getEvents()`
