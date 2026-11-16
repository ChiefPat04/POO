# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/powerband/` (paquetes `instrument`, `command`, `sc`, `editor`, `model`, `exception`, `util`; la persistencia de `storage` está en el mapa de database)

## Implemented
- Patrón Bridge, abstracción: `src/powerband/instrument/Instrument.java`
- Patrón Bridge, implementación (comandos de synth + observador): `src/powerband/command/`
- Cliente OSC por UDP hacia scsynth: `src/powerband/sc/OscClient.java`
- Grabador del editor (ft-5): `src/powerband/editor/Recorder.java`
- Contrato del índice (lo implementa ft-6): `src/powerband/storage/IRecordingIndexer.java`
- Evento musical inmutable, enums, encabezado de grabación y entrada de lista: `src/powerband/model/`
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
- `allNotesOff()` — apaga cada nota encendida pasando por `execute`, así los listeners reciben los `NOTE_OFF` (`throws SynthConnectionException`)

### Recorder
`ISynthEventListener` registrado en los 3 comandos; estados `IDLE` → `RECORDING` → `PENDING_NAME`; todo `synchronized`. `src/powerband/editor/Recorder.java`
- `Recorder(RecordingStorage pStorage, IRecordingIndexer pIndexer, ISynthCommand pPiano, ISynthCommand pBass, OrganCommand pOrgan)`
- `start()` — escribe el encabezado provisional y guarda la hora de inicio (`throws StorageException`)
- `stop()` — `allNotesOff` del órgano, fija la posición de fin, relanza una falla de escritura (`throws StorageException, SynthConnectionException`)
- `save(String pName)` — valida, revisa unicidad en disco, reescribe encabezado, agrega a la lista, llama al indexador (`throws StorageException, DuplicateRecordingNameException`; nombre inválido → `IllegalArgumentException`)
- `discard()` — recorta `recordings.music` a donde estaba antes de `start` (`throws StorageException`)
- `onSynthEvent(SynthEvent pEvent)` — si está grabando, escribe el evento con tiempo relativo (mínimo 0)
- `isRecording()`, `isPendingName()`, `getStartTimeMs()`

### IRecordingIndexer
Interfaz del índice por nombre. `src/powerband/storage/IRecordingIndexer.java`
- `add(String pName, long pHeaderPosition)` — (`throws StorageException`)

### RecordingHeader
Inmutable. `src/powerband/model/RecordingHeader.java`
- `RecordingHeader(String pName, long pStartPosition, long pEndPosition)`; `getName()`, `getStartPosition()`, `getEndPosition()`

### ListEntry
Inmutable. `src/powerband/model/ListEntry.java`
- `ListEntry(String pName, long pCreatedAtMs)`; `getName()`, `getCreatedAtMs()`

### OscClient
Cliente OSC UDP, `AutoCloseable`. `src/powerband/sc/OscClient.java`
- `OscClient(String pHost, int pPort)` — valida puerto, resuelve host, abre socket (`throws SynthConnectionException`)
- `nextNodeId()` — `AtomicInteger` desde `FIRST_NODE_ID`
- `sNew(String pDefName, int pNodeId, Object... pControls)` — envía `/s_new` (`throws SynthConnectionException`)
- `nSet(int pNodeId, Object... pControls)` — envía `/n_set` (`throws SynthConnectionException`)
- `encode(String pPath, Object... pArgs)` — estático; bytes OSC (Integer/Float/String; otro → `IllegalArgumentException`)
- `close()` — cierra el socket (no apaga notas; para eso está `OrganCommand.allNotesOff`)

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

### StorageException / DuplicateRecordingNameException
Checked, extienden `PowerBandException`: falla de E/S en los archivos de grabaciones, y nombre ya existente (`DuplicateRecordingNameException(String pName)`). `src/powerband/exception/`

### IConstants
Conexión, OSC, synths, parámetros, valores por defecto (`List.of` inmutables para el órgano), archivos y formato binario de las grabaciones, y mensajes de error. `src/powerband/util/IConstants.java`
