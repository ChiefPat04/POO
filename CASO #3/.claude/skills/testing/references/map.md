# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/powerband/` (JUnit 5, `mvn test`)

## Implemented
- Pruebas de ft-1 sin SuperCollider: un `DatagramSocket` local en puerto efímero hace de scsynth.
- Pruebas de ft-5 (grabación) sobre archivos en `@TempDir`; nunca tocan `data/`.
- Pruebas de ft-3 (control PS5) con un lector falso; no cargan Jamepad. `JamepadReader` se prueba a mano con `mvn compile exec:java`.

## Test classes
- `src/test/powerband/sc/OscClientTest.java` — formato OSC, nodeIds únicos con hilos, puerto inválido, host `.invalid`
- `src/test/powerband/model/SynthEventTest.java` — fábricas, valores por defecto, `withTimeMs`
- `src/test/powerband/command/PianoCommandTest.java` — mensaje `classicPiano`, rechazo de otros synths
- `src/test/powerband/command/BassCommandTest.java` — mensaje `stringBass`, rechazo de otros synths
- `src/test/powerband/command/OrganCommandTest.java` — noteOn/noteOff, reinicio, `midiToFreq`, concurrencia, `allNotesOff`
- `src/test/powerband/storage/RecordingStorageTest.java` — formato binario, tamaño fijo, relleno del nombre, reescritura del encabezado, lista, `isValidName`
- `src/test/powerband/editor/RecorderTest.java` — flujo start/stop/save/discard, tiempo relativo, noteOff al detener, nombre duplicado o inválido, concurrencia, falla de escritura
- `src/test/powerband/command/SynthCommandListenerTest.java` — notificación y remoción de listeners
- `src/test/powerband/instrument/InstrumentTest.java` — delegación del Bridge y `setCommand`
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

### FakeIndexer
`IRecordingIndexer` que guarda las llamadas. `src/test/powerband/editor/helpers/FakeIndexer.java`
- `add(String pName, long pHeaderPosition)`
- `getNames()`, `getPositions()`

### FakeGamepadReader
`IGamepadReader` falso. `src/test/powerband/instrument/helpers/FakeGamepadReader.java`
- `setState(GamepadState pState)`, `setFailOnOpen(boolean pFail)`, `getReadCount()`, `isClosed()`

### RecordingCommand
`ISynthCommand` que guarda eventos (lista segura para hilos). `src/test/powerband/instrument/helpers/RecordingCommand.java`
- `getEvents()`
