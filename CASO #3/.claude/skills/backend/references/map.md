# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/powerband/` (paquetes `instrument`, `command`, `sc`, `model`, `exception`, `util`)

## Implemented
- Patrón Bridge, abstracción: `src/powerband/instrument/Instrument.java`
- Patrón Bridge, implementación (comandos de synth + observador): `src/powerband/command/`
- Cliente OSC por UDP hacia scsynth: `src/powerband/sc/OscClient.java`
- Evento musical inmutable y enums: `src/powerband/model/`
- Excepciones checked: `src/powerband/exception/`
- Constantes: `src/powerband/util/IConstants.java`

## Entrypoints
- `src/powerband/PowerBandMain.java` (por ahora solo imprime `APP_NAME`)

## Classes
### Instrument
Abstracta; dispositivo que delega en un `ISynthCommand`. `src/powerband/instrument/Instrument.java`
- `setCommand(ISynthCommand pCommand)` — cambia el synth destino (null → `IllegalArgumentException`)
- `getCommand()` — comando actual
- `start()` — abstracto; empieza a leer el dispositivo (`throws PowerBandException`)
- `stop()` — abstracto; deja de leer el dispositivo
- `play(SynthEvent pEvent)` — protegido; ejecuta el evento en el comando

### ISynthCommand
Interfaz del Bridge para invocar un synth. `src/powerband/command/ISynthCommand.java`
- `execute(SynthEvent pEvent)` — envía y notifica (`throws SynthConnectionException`)
- `addListener(ISynthEventListener pListener)` / `removeListener(ISynthEventListener pListener)`

### ISynthEventListener
Observador de comandos enviados (lo usará el editor). `src/powerband/command/ISynthEventListener.java`
- `onSynthEvent(SynthEvent pEvent)` — recibe cada evento enviado, con `timeMs` absoluto

### SynthCommand
Abstracta; listeners en `CopyOnWriteArrayList` y template method. `src/powerband/command/SynthCommand.java`
- `execute(SynthEvent pEvent)` — final: valida, estampa `System.currentTimeMillis()`, `send`, notifica si se envió
- `addListener(...)` / `removeListener(...)`

### PianoCommand
`/s_new classicPiano` con `note`, `durMs`, `third`, `amp`; solo `PIANO`/`TRIGGER`. `src/powerband/command/PianoCommand.java`
- `PianoCommand(OscClient pClient)`

### BassCommand
`/s_new stringBass` con `note`, `durMs`, `amp`; solo `BASS`/`TRIGGER`. `src/powerband/command/BassCommand.java`
- `BassCommand(OscClient pClient)`

### OrganCommand
`organ70s`: noteOn `/s_new` (freq, amp, r1..r3, a1..a3), noteOff `/n_set gate 0`; mapa nota→nodo con métodos `synchronized`. `src/powerband/command/OrganCommand.java`
- `OrganCommand(OscClient pClient)`
- `midiToFreq(int pNote)` — estático; 440 * 2^((nota - 69) / 12) como float

### OscClient
Cliente OSC UDP, `AutoCloseable`. `src/powerband/sc/OscClient.java`
- `OscClient(String pHost, int pPort)` — valida puerto, resuelve host, abre socket (`throws SynthConnectionException`)
- `nextNodeId()` — `AtomicInteger` desde `FIRST_NODE_ID`
- `sNew(String pDefName, int pNodeId, Object... pControls)` — envía `/s_new` (`throws SynthConnectionException`)
- `nSet(int pNodeId, Object... pControls)` — envía `/n_set` (`throws SynthConnectionException`)
- `encode(String pPath, Object... pArgs)` — estático; bytes OSC (Integer/Float/String; otro → `IllegalArgumentException`)
- `close()` — cierra el socket (no apaga notas; eso es ft-5)

### SynthEvent
Inmutable; constructor privado, fábricas validadas. `src/powerband/model/SynthEvent.java`
- `piano(int pNote, int pDurationMs, int pThird[, float pAmp])`
- `bass(int pNote, int pDurationMs[, float pAmp])`
- `organOn(int pNote)` / `organOn(int pNote, float pAmp, float[] pRatios, float[] pAmps)` — arreglos de 3 o `IllegalArgumentException`
- `organOff(int pNote)`
- `withTimeMs(long pTimeMs)` — copia con otro tiempo
- getters: `getTimeMs`, `getSynth`, `getAction`, `getNote`, `getDurationMs`, `getThird`, `getAmp`, `getR1..getR3`, `getA1..getA3`

### SynthType / SynthAction
Enums `PIANO, BASS, ORGAN` y `TRIGGER, NOTE_ON, NOTE_OFF`. `src/powerband/model/`

### PowerBandException / SynthConnectionException
Checked; base y error de conexión local (UDP no detecta scsynth apagado). `src/powerband/exception/`

### IConstants
Conexión, OSC, synths, parámetros, valores por defecto (`List.of` inmutables para el órgano) y mensajes de error. `src/powerband/util/IConstants.java`
