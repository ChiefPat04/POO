# Testing

After you change this layer, update this file so the next run stays current.

Source: `src/test/powerband/` (JUnit 5, `mvn test`)

## Implemented
- Pruebas de ft-1 sin SuperCollider: un `DatagramSocket` local en puerto efímero hace de scsynth.

## Test classes
- `src/test/powerband/sc/OscClientTest.java` — formato OSC, nodeIds únicos con hilos, puerto inválido, host `.invalid`
- `src/test/powerband/model/SynthEventTest.java` — fábricas, valores por defecto, `withTimeMs`
- `src/test/powerband/command/PianoCommandTest.java` — mensaje `classicPiano`, rechazo de otros synths
- `src/test/powerband/command/BassCommandTest.java` — mensaje `stringBass`, rechazo de otros synths
- `src/test/powerband/command/OrganCommandTest.java` — noteOn/noteOff, reinicio, `midiToFreq`, concurrencia
- `src/test/powerband/command/SynthCommandListenerTest.java` — notificación y remoción de listeners
- `src/test/powerband/instrument/InstrumentTest.java` — delegación del Bridge y `setCommand`

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

### RecordingCommand
`ISynthCommand` que guarda eventos. `src/test/powerband/instrument/helpers/RecordingCommand.java`
- `getEvents()`
