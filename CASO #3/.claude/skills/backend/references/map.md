# Backend

After you change this layer, update this file so the next run stays current.

Source: `src/powerband/` (paquetes `instrument`, `command`, `sc`, `editor`, `model`, `exception`, `util`; la persistencia de `storage` está en el mapa de database)

## Implemented
- Patrón Bridge, abstracción: `src/powerband/instrument/Instrument.java`
- Micrófono → órgano (ft-2): `src/powerband/instrument/MicrophoneInstrument.java`, con la captura detrás de `IAudioSource` (`MicrophoneSource` usa `javax.sound.sampled`) y la detección de nota en `PitchDetector.java`
- Control PS5 → bajo (ft-3): `src/powerband/instrument/GamepadInstrument.java`, con el dispositivo detrás de `IGamepadReader` (`JamepadReader` usa Jamepad)
- Webcam → piano (ft-4): `src/powerband/instrument/WebcamInstrument.java`, con la cámara detrás de `IFrameSource` (`WebcamSource` usa Webcam Capture) y la detección de movimiento en `HandTracker.java`
- Patrón Bridge, implementación (comandos de synth + observador): `src/powerband/command/`
- Cliente OSC por UDP hacia scsynth: `src/powerband/sc/OscClient.java`
- Grabador del editor (ft-5): `src/powerband/editor/Recorder.java`
- Contrato del índice (lo implementa `RecordingIndex`, ft-6; ver mapa de database): `src/powerband/storage/IRecordingIndexer.java`
- Evento musical inmutable, enums, encabezado de grabación y entrada de lista: `src/powerband/model/`
- Excepciones checked: `src/powerband/exception/`
- Constantes: `src/powerband/util/IConstants.java`

## Entrypoints
- `src/powerband/PowerBandMain.java` (provisional hasta ft-8: `OscClient`, los 3 comandos, `GamepadInstrument`, `MicrophoneInstrument` y `WebcamInstrument`; si uno no inicia se informa y los demás siguen; termina con Enter, luego `stop()` de los iniciados y `allNotesOff()`)

## Classes
### Instrument
Abstracta; dispositivo que delega en un `ISynthCommand`. `src/powerband/instrument/Instrument.java`
- `setCommand(ISynthCommand pCommand)` — cambia el synth destino (null → `IllegalArgumentException`)
- `getCommand()` — comando actual
- `start()` — abstracto; empieza a leer el dispositivo (`throws PowerBandException`)
- `stop()` — abstracto; deja de leer el dispositivo
- `play(SynthEvent pEvent)` — protegido; ejecuta el evento en el comando

### GamepadInstrument
`Instrument` del control: hilo daemon que lee cada `GAMEPAD_POLL_INTERVAL_MS`; la base es un entero de pasos de Do mayor desde `GAMEPAD_ROOT_NOTE`. `src/powerband/instrument/GamepadInstrument.java`
- `GamepadInstrument(ISynthCommand pCommand, IGamepadReader pReader)`
- `start()` — `reader.open()` y arranca el hilo (`throws DeviceException`; ya iniciado → `IllegalStateException`)
- `stop()` — detiene el hilo y cierra el lector; idempotente
- `process(GamepadState pState)` — una lectura: stick/L1/R1 mueven la base, luego cada botón recién presionado dispara `SynthEvent.bass` con `durMs` según R2 (`throws SynthConnectionException`)
- `stepToNote(int pStep)` — estático; paso de la escala → nota MIDI limitada a 28–55

### MicrophoneInstrument
`Instrument` del micrófono: hilo daemon que lee ventanas de `AUDIO_BLOCK_SIZE`; una sola nota encendida a la vez, con histéresis de volumen. `src/powerband/instrument/MicrophoneInstrument.java`
- `MicrophoneInstrument(ISynthCommand pCommand, IAudioSource pSource)`
- `start()` — `source.open()` y arranca el hilo (`throws DeviceException`; ya iniciado → `IllegalStateException`)
- `stop()` — detiene el hilo, envía `organOff` de la nota encendida y cierra la fuente; idempotente
- `process(float[] pBlock)` — una ventana: nota estable y volumen ≥ `MIC_ON_THRESHOLD` → `organOn`; nota estable distinta → `organOff` + `organOn`; silencio sostenido → `organOff` (`throws SynthConnectionException`)

### IAudioSource
Fuente de bloques de muestras entre -1 y 1. `src/powerband/instrument/IAudioSource.java`
- `open()` — (`throws DeviceException`)
- `read(float[] pBlock)` — llena el bloque; `false` si no hay audio; nunca lanza
- `close()`

### MicrophoneSource
`IAudioSource` real: `TargetDataLine` a 44100 Hz, mono, 16 bits, little-endian. `src/powerband/instrument/MicrophoneSource.java`
- `open()`, `read(float[] pBlock)`, `close()`

### PitchDetector
Autocorrelación normalizada con interpolación parabólica. `src/powerband/instrument/PitchDetector.java`
- `PitchDetector(float pSampleRate)`
- `detectFrequency(float[] pSamples)` — Hz, o `NO_PITCH` si no es periódica o sale de `PITCH_MIN_FREQ`..`PITCH_MAX_FREQ`
- `rms(float[] pSamples)` — volumen de la ventana
- `toMidiNote(float pFrequency)` — semitono más cercano

### WebcamInstrument
`Instrument` de la webcam: hilo daemon que lee un cuadro cada `CAM_POLL_INTERVAL_MS`; un disparo por golpe, con histéresis de movimiento. `src/powerband/instrument/WebcamInstrument.java`
- `WebcamInstrument(ISynthCommand pCommand, IFrameSource pSource)`
- `start()` — `source.open()` y arranca el hilo (`throws DeviceException`; ya iniciado → `IllegalStateException`)
- `stop()` — detiene el hilo y cierra la fuente; idempotente
- `process(HandReading pReading)` — movimiento ≥ `CAM_STRIKE_MOTION` y armado → `SynthEvent.piano` (nota por altura, tercera por lado, amp por movimiento, `CAM_DUR_MS`); se rearma con movimiento < `CAM_REARM_MOTION` o mano ausente (`throws SynthConnectionException`)
- `heightToNote(float pY)` — estático; altura 0..1 (0 = arriba) → nota de Do mayor 48–72 en 15 franjas

### IFrameSource
Fuente de cuadros de video. `src/powerband/instrument/IFrameSource.java`
- `open()` — (`throws DeviceException`)
- `read()` — cuadro `BufferedImage` o `null` si no hay; nunca lanza
- `close()`

### WebcamSource
`IFrameSource` real: `Webcam.getDefault()` a QVGA (320×240) con Webcam Capture 0.3.12. `src/powerband/instrument/WebcamSource.java`
- `open()`, `read()`, `close()`

### HandTracker
Diferencia entre cuadros en una cuadrícula gris de `CAM_GRID_WIDTH × CAM_GRID_HEIGHT`; centro de los píxeles que cambiaron, espejado si `CAM_MIRROR`. `src/powerband/instrument/HandTracker.java`
- `HandTracker(int pGridWidth, int pGridHeight)`
- `track(BufferedImage pFrame)` — `HandReading` con x, y y fracción que cambió; ausente en el primer cuadro o si el cambio sale de `CAM_MIN_MOTION`..`CAM_MAX_MOTION`
- `reset()` — olvida el cuadro anterior

### HandReading
Inmutable: dónde se movió la mano en un cuadro. `src/powerband/model/HandReading.java`
- `HandReading(boolean pPresent, float pX, float pY, float pMotion)`; `absent()` — fábrica estática
- getters: `isPresent`, `getX`, `getY`, `getMotion`

### IGamepadReader
Lectura del control. `src/powerband/instrument/IGamepadReader.java`
- `open()` — inicializa; falla si no hay control (`throws DeviceException`)
- `read()` — foto del estado; nunca lanza, sin control devuelve `GamepadState.disconnected()`
- `close()` — libera el dispositivo

### JamepadReader
`IGamepadReader` real con Jamepad/SDL (`a/b/x/y` = equis/círculo/cuadrado/triángulo). `src/powerband/instrument/JamepadReader.java`
- `open()`, `read()`, `close()` (`quitSDLGamepad`)

### GamepadState
Foto inmutable del control. `src/powerband/model/GamepadState.java`
- `GamepadState(boolean pCross, boolean pCircle, boolean pSquare, boolean pTriangle, boolean pL1, boolean pR1, float pLeftStickX, float pRightTrigger, boolean pConnected)`
- `disconnected()` / `idle()` — fábricas estáticas
- getters: `isCross`, `isCircle`, `isSquare`, `isTriangle`, `isL1`, `isR1`, `getLeftStickX`, `getRightTrigger`, `isConnected`

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

### IndexEntry
Inmutable: registro de `recordings.index`. `src/powerband/model/IndexEntry.java`
- `IndexEntry(String pName, long pHeaderPosition)`; `getName()`, `getHeaderPosition()`

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
- `organOn(int pNote)` / `organOn(int pNote, float pAmp)` / `organOn(int pNote, float pAmp, float[] pRatios, float[] pAmps)` — arreglos de 3 o `IllegalArgumentException`
- `organOff(int pNote)`
- `withTimeMs(long pTimeMs)` — copia con otro tiempo
- getters: `getTimeMs`, `getSynth`, `getAction`, `getNote`, `getDurationMs`, `getThird`, `getAmp`, `getR1..getR3`, `getA1..getA3`

### SynthType / SynthAction
Enums `PIANO, BASS, ORGAN` y `TRIGGER, NOTE_ON, NOTE_OFF`. `src/powerband/model/`

### PowerBandException / SynthConnectionException
Checked; base y error de conexión local (UDP no detecta scsynth apagado). `src/powerband/exception/`

### DeviceException
Checked, extiende `PowerBandException`: dispositivo sin conectar o que no inicializa. `src/powerband/exception/DeviceException.java`

### StorageException / DuplicateRecordingNameException
Checked, extienden `PowerBandException`: falla de E/S en los archivos de grabaciones, y nombre ya existente (`DuplicateRecordingNameException(String pName)`). `src/powerband/exception/`

### IConstants
Conexión, OSC, synths, parámetros, valores por defecto (`List.of` inmutables para el órgano), archivos y formato binario de las grabaciones (incluido el índice y el Balanced Merge Sort), escala y rango del bajo, control PS5, captura de audio, detección de tono, reglas del micrófono, webcam → piano (rango, cuadrícula, umbrales de movimiento, hilo), y mensajes de consola y de error. `src/powerband/util/IConstants.java`
