---
id: ft-1
layer: backend
status: draft
---

# Contratos Bridge y conexión SuperCollider

## General description

- Crea las dos jerarquías del Bridge: `Instrument` (abstracción, sin dispositivo real) e `ISynthCommand` (implementación) con `PianoCommand`, `BassCommand` y `OrganCommand`.
- `OscClient` codifica y envía mensajes OSC `/s_new` y `/n_set` por UDP a `scsynth`, basado en `recursos/supercollider/BandClient.java`.
- Cada comando ejecutado crea un `SynthEvent` con la hora de envío y lo notifica a los `ISynthEventListener` registrados (observador para el editor).
- Agrega `PowerBandException` y `SynthConnectionException`.

## Inputs

- `SynthEvent` (desde un `Instrument` o, en ft-7, desde `Player`): synth, acción, nota MIDI, `durMs`, tercera, `r1..r3`, `a1..a3`. El tiempo lo pone el comando.
- Host y puerto de `scsynth`: `IConstants.SC_HOST` (`127.0.0.1`), `IConstants.SC_PORT` (`57110`).

## Outputs

- Datagramas UDP OSC hacia `scsynth`:
  - Piano: `/s_new "classicPiano" <nodeId> 0 1 "note" i "durMs" i "third" i`
  - Bajo: `/s_new "stringBass" <nodeId> 0 1 "note" i "durMs" i`
  - Órgano noteOn: `/s_new "organ70s" <nodeId> 0 1 "freq" f "r1" f "r2" f "r3" f "a1" f "a2" f "a3" f`
  - Órgano noteOff: `/n_set <nodeId> "gate" 0`
- `SynthEvent` notificado a cada `ISynthEventListener.onSynthEvent(SynthEvent pEvent)`, con `timeMs` = `System.currentTimeMillis()` al enviar.
- `SynthConnectionException` cuando el socket no se puede crear, el host no resuelve o falla el envío.

## Test criteria

- `OscClient.encode` produce bytes idénticos al algoritmo de `BandClient.send`: strings ASCII terminados en null y rellenados a múltiplo de 4, type tag `,` + `i`/`f`/`s`, enteros big-endian, floats como `Float.floatToIntBits`.
- `encode` con un tipo no soportado lanza `IllegalArgumentException`.
- Cada comando envía exactamente los mensajes de **Outputs** (path, defName, args y orden).
- Los `nodeId` empiezan en `IConstants.FIRST_NODE_ID` (10000) y nunca se repiten, aun con varios hilos.
- `OrganCommand`: `noteOn` sobre una nota ya encendida envía primero `/n_set gate 0` al nodo viejo; `noteOff` de una nota no encendida no envía nada ni notifica.
- `freq` del órgano = `440 * 2^((nota - 69) / 12)` como `float`.
- Todo listener registrado recibe un evento por mensaje enviado; un listener removido ya no recibe.
- `Instrument.setCommand` cambia el synth destino sin cambiar la subclase de `Instrument`.

## Assets

- Contratos:
  - `ISynthCommand`: `void execute(SynthEvent pEvent) throws SynthConnectionException`, `void addListener(ISynthEventListener pListener)`, `void removeListener(ISynthEventListener pListener)`.
  - `ISynthEventListener`: `void onSynthEvent(SynthEvent pEvent)`.
  - `Instrument` (abstracta): `Instrument(ISynthCommand pCommand)`, `setCommand(ISynthCommand pCommand)`, `getCommand()`, `protected void play(SynthEvent pEvent) throws SynthConnectionException`, `public abstract void start() throws PowerBandException`, `public abstract void stop()`.
  - `OscClient implements AutoCloseable`: `OscClient(String pHost, int pPort) throws SynthConnectionException`, `int nextNodeId()`, `void sNew(String pDefName, int pNodeId, Object... pControls)`, `void nSet(int pNodeId, Object... pControls)`, `public static byte[] encode(String pPath, Object... pArgs)`, `close()`.
- Modelo: `SynthEvent` inmutable (`timeMs`, `SynthType synth`, `SynthAction action`, `note`, `durationMs`, `third`, `r1..r3`, `a1..a3`) con `withTimeMs(long pTimeMs)`; enums `SynthType { PIANO, BASS, ORGAN }` y `SynthAction { TRIGGER, NOTE_ON, NOTE_OFF }`.
- Claves nuevas en `IConstants`: `SC_HOST`, `SC_PORT`, `OSC_S_NEW`, `OSC_N_SET`, `ADD_TO_HEAD` (0), `DEFAULT_GROUP` (1), `FIRST_NODE_ID` (10000), `OSC_ALIGNMENT` (4), nombres de synth (`classicPiano`, `stringBass`, `organ70s`), nombres de parámetro (`note`, `durMs`, `third`, `freq`, `r1..r3`, `a1..a3`, `gate`), `GATE_OFF` (0), `THIRD_MAJOR` (4), `THIRD_MINOR` (3), `MIDI_A4` (69), `FREQ_A4` (440.0), `SEMITONES_PER_OCTAVE` (12.0), ratios y amplitudes por defecto del órgano (`1, 2, 3` / `1, 0.5, 0.25`).

## Implementation guidelines

- Archivos:
  - `src/powerband/instrument/Instrument.java`
  - `src/powerband/command/ISynthCommand.java`, `SynthCommand.java` (abstracta: lista de listeners y notificación), `PianoCommand.java`, `BassCommand.java`, `OrganCommand.java`, `ISynthEventListener.java`
  - `src/powerband/sc/OscClient.java`
  - `src/powerband/model/SynthEvent.java`, `SynthType.java`, `SynthAction.java`
  - `src/powerband/exception/PowerBandException.java` (checked), `SynthConnectionException.java` (extiende `PowerBandException`)
  - `src/powerband/util/IConstants.java`
- Java 17 (`maven.compiler.release`), solo stdlib (`DatagramSocket`, `ByteArrayOutputStream`). Sin dependencias nuevas.
- `SynthCommand.execute` es template method: estampa el tiempo, llama `protected abstract void send(SynthEvent pEvent)`, y notifica solo si se envió algo.
- Los comandos reciben el `OscClient` por constructor; nunca lo crean.
- Hilos: los instrumentos llaman desde hilos propios. `nextNodeId` con `AtomicInteger`; listeners en `CopyOnWriteArrayList`; mapa nota→nodo del órgano sincronizado.
- `IOException` del socket se envuelve en `SynthConnectionException`; nunca `catch` vacío (el `close` de `BandClient` no se copia así).
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- `BandClient.java` es solo referencia; no se compila ni se mueve.
- Fuera de alcance: dispositivos reales (ft-2..ft-4), `allNotesOff` para el grabador (ft-5), `DeviceException`/`StorageException` (las agrega la feature que las use).

## Business workflow

1. Se construye `OscClient(SC_HOST, SC_PORT)`; si falla, `SynthConnectionException`.
2. Se crean `PianoCommand`, `BassCommand`, `OrganCommand` con ese `OscClient`.
3. El editor registra su `ISynthEventListener` en cada comando.
4. Una subclase de `Instrument` arma un `SynthEvent` y llama `play`, que delega en su `ISynthCommand`.
5. El comando estampa `timeMs`, pide un `nodeId`, envía `/s_new` o `/n_set` y notifica el evento a los listeners.
6. Al cerrar la app se cierra `OscClient` con try-with-resources.
