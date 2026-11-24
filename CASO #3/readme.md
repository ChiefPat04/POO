# Power Band (Caso #3 - POO)

Programa de creación e interacción musical: 3 instrumentos físicos controlan 3 synths de SuperCollider, y un editor graba, guarda, carga y reproduce la música.

| Dispositivo | Instrumento (Java) | Comando | Synth (SuperCollider) |
|---|---|---|---|
| Control PS5 (DualSense) | `GamepadInstrument` | `BassCommand` | `stringBass` |
| Micrófono (violín, guitarra, voz) | `MicrophoneInstrument` | `OrganCommand` | `organ70s` |
| Webcam | `WebcamInstrument` | `PianoCommand` | `classicPiano` |

## Prerrequisitos

| Herramienta | Verificar |
|---|---|
| JDK (versión de `pom.xml`) | `java -version` |
| Apache Maven 3.9+ | `mvn -version` |
| SuperCollider | https://supercollider.github.io/downloads.html |
| Claude Code | `claude --version` |

## Antes de arrancar (para que la demo no falle)

1. **Conectar los audífonos y el control antes de abrir SuperCollider.** SuperCollider elige la salida de audio al encender el servidor; si se cambia un dispositivo con el servidor encendido, el servidor se cuelga (los números de abajo a la derecha quedan en naranja).
2. **Usar audífonos.** Si el órgano sale por los parlantes, el micrófono lo escucha y la nota se queda pegada.
3. **Revisar el micrófono predeterminado de Windows.** Al conectar el control, Windows puede elegir el micrófono del DualSense como predeterminado. En *Configuración de sonido → Entrada*, elegir el micrófono real. El programa usa el micrófono predeterminado.
4. **Permiso del micrófono:** *Configuración → Privacidad y seguridad → Micrófono* → permitir a las aplicaciones de escritorio.
5. **Cerrar Steam** (puede quedarse con el control) y cualquier programa que esté usando la webcam.

## Preparar SuperCollider

1. Abrir `recursos/supercollider/myband.sc` en el IDE de SuperCollider.
2. Evaluar el bloque entre paréntesis (Ctrl+Enter).
3. Evaluar esta línea, para que SuperCollider no use el micrófono (solo genera sonido y así no se pelea con Java por él):
   ```supercollider
   Server.default.options.numInputBusChannels = 0;
   ```
4. Evaluar `~myband.load;` y esperar el mensaje `myband: classicPiano, stringBass, organ70s loaded`. Los números de abajo a la derecha deben quedar en **verde**.

Si los números quedan en naranja: *Server → Kill All Servers* y repetir los pasos 2 a 4.

## Correr el programa

Desde esta carpeta (donde está `pom.xml`):

```powershell
mvn compile exec:java
```

Se abre la ventana del editor. La barra de abajo muestra qué instrumentos iniciaron y cuáles fallaron (si uno falla, los demás siguen).

- **Grabar** → tocar → **Detener grabación** → escribir el nombre (máximo 32 caracteres, sin tildes ni ñ; no se puede repetir).
- Elegir una grabación de la lista → **Cargar** (búsqueda binaria en el índice) → **Reproducir**.

## Pruebas

```powershell
mvn clean verify
```

Las pruebas no necesitan SuperCollider ni dispositivos: usan un receptor UDP falso y dispositivos falsos.

## Flujo de trabajo con IA (spec driven development)

1. `/generate-feature-spec <descripción>` → crea `specs/<capa>/ft-N-*.md` en `draft`.
2. Revisar y editar la spec a mano, luego cambiar a `status: approved`.
3. `/develop-feature ft-N` → implementa y corre las pruebas.
4. `/update-current-development` → actualiza los mapas de `.claude/skills/`.

---

# Diagramas

## 1. Vista general: cómo se conecta todo

Cada dispositivo lo lee su instrumento en un hilo propio. El instrumento arma un `SynthEvent` y se lo pasa a su comando (Bridge). El comando lo manda a SuperCollider por OSC/UDP y avisa a sus observadores: el grabador (que lo escribe en disco) y el monitor de la ventana (que lo muestra). El reproductor lee los comandos de disco y los manda **por los mismos comandos**.

```mermaid
flowchart LR
    subgraph DEV["Dispositivos"]
        PS5["Control PS5"]
        MIC["Micrófono"]
        CAM["Webcam"]
    end

    subgraph ABS["Abstracción del Bridge (instrument)"]
        GI["GamepadInstrument"]
        MI["MicrophoneInstrument"]
        WI["WebcamInstrument"]
    end

    subgraph IMP["Implementación del Bridge (command)"]
        BC["BassCommand"]
        OC["OrganCommand"]
        PC["PianoCommand"]
    end

    OSC["OscClient<br/>(UDP 57110)"]
    SC[("SuperCollider<br/>scsynth")]

    subgraph ED["Editor"]
        REC["Recorder"]
        PLY["Player"]
        LIB["RecordingLibrary"]
        CTRL["EditorController"]
    end

    subgraph ST["Almacenamiento en disco (storage)"]
        RS["RecordingStorage"]
        RI["RecordingIndex<br/>Balanced Merge Sort<br/>+ búsqueda binaria"]
        MUSIC[("recordings.music")]
        LIST[("recordings.list")]
        INDEX[("recordings.index")]
    end

    subgraph UI["Ventana (ui)"]
        WIN["EditorWindow"]
        MON["MonitorPanel"]
    end

    PS5 -->|"JamepadReader"| GI
    MIC -->|"MicrophoneSource"| MI
    CAM -->|"WebcamSource + HandTracker"| WI

    GI -->|"play(SynthEvent)"| BC
    MI -->|"play(SynthEvent)"| OC
    WI -->|"play(SynthEvent)"| PC

    BC --> OSC
    OC --> OSC
    PC --> OSC
    OSC -->|"/s_new, /n_set"| SC

    BC -. "onSynthEvent" .-> REC
    OC -. "onSynthEvent" .-> REC
    PC -. "onSynthEvent" .-> REC
    BC -. "onSynthEvent" .-> MON
    OC -. "onSynthEvent" .-> MON
    PC -. "onSynthEvent" .-> MON

    REC --> RS
    REC -->|"add(nombre, posición)"| RI
    RS --> MUSIC
    RS --> LIST
    RI --> INDEX

    LIB --> RS
    LIB -->|"find(nombre)"| RI
    PLY -->|"readEvent"| RS
    PLY -->|"execute"| BC
    PLY -->|"execute"| OC
    PLY -->|"execute"| PC

    WIN <--> CTRL
    CTRL --> REC
    CTRL --> PLY
    CTRL --> LIB
    WIN --- MON
```

Líneas punteadas = patrón **Observador** (`ISynthEventListener`). Líneas sólidas = llamadas directas.

## 2. Patrón Bridge: instrumentos y comandos

La **abstracción** (`Instrument`) y la **implementación** (`ISynthCommand`) son dos jerarquías separadas. Cada instrumento *tiene* un comando (composición), no hereda de él, y se puede cambiar con `setCommand` sin tocar la clase del instrumento. Cada instrumento lee su dispositivo detrás de una interfaz (`IGamepadReader`, `IAudioSource`, `IFrameSource`), así las pruebas usan dispositivos falsos.

```mermaid
classDiagram
    direction LR

    class Instrument {
        <<abstract>>
        -ISynthCommand command
        #Instrument(ISynthCommand pCommand)
        +setCommand(ISynthCommand pCommand) void
        +getCommand() ISynthCommand
        +start()* void
        +stop()* void
        #play(SynthEvent pEvent) void
    }

    class GamepadInstrument {
        -IGamepadReader reader
        -int baseStep
        -boolean stickArmed
        -GamepadState previous
        -Thread thread
        +start() void
        +stop() void
        +process(GamepadState pState) void
        +stepToNote(int pStep)$ int
        -pollLoop() void
    }

    class MicrophoneInstrument {
        -IAudioSource source
        -PitchDetector detector
        -int activeNote
        -int candidateNote
        -int candidateCount
        -int quietCount
        -Thread thread
        +start() void
        +stop() void
        +process(float[] pBlock) void
        -readLoop() void
    }

    class WebcamInstrument {
        -IFrameSource source
        -HandTracker tracker
        -boolean armed
        -Thread thread
        +start() void
        +stop() void
        +process(HandReading pReading) void
        +heightToNote(float pY)$ int
        -readLoop() void
    }

    class IGamepadReader {
        <<interface>>
        +open() void
        +read() GamepadState
        +close() void
    }
    class JamepadReader
    class IAudioSource {
        <<interface>>
        +open() void
        +read(float[] pBlock) boolean
        +close() void
    }
    class MicrophoneSource
    class IFrameSource {
        <<interface>>
        +open() void
        +read() BufferedImage
        +close() void
    }
    class WebcamSource
    class PitchDetector {
        +detectFrequency(float[] pSamples) float
        +rms(float[] pSamples) float
        +toMidiNote(float pFrequency) int
    }
    class HandTracker {
        -int[] previous
        +track(BufferedImage pFrame) HandReading
        +reset() void
    }

    class ISynthCommand {
        <<interface>>
        +execute(SynthEvent pEvent) void
        +addListener(ISynthEventListener pListener) void
        +removeListener(ISynthEventListener pListener) void
    }

    class SynthCommand {
        <<abstract>>
        -OscClient client
        -List~ISynthEventListener~ listeners
        +execute(SynthEvent pEvent) void
        #validate(SynthEvent pEvent)* void
        #send(SynthEvent pEvent)* boolean
        -notifyListeners(SynthEvent pEvent) void
    }

    class PianoCommand {
        #validate(SynthEvent pEvent) void
        #send(SynthEvent pEvent) boolean
    }
    class BassCommand {
        #validate(SynthEvent pEvent) void
        #send(SynthEvent pEvent) boolean
    }
    class OrganCommand {
        -Map activeNodes
        +allNotesOff() void
        +midiToFreq(int pNote)$ float
        #validate(SynthEvent pEvent) void
        #send(SynthEvent pEvent) boolean
    }

    class ISynthEventListener {
        <<interface>>
        +onSynthEvent(SynthEvent pEvent) void
    }

    class OscClient {
        -DatagramSocket socket
        -AtomicInteger nodeIdCounter
        +nextNodeId() int
        +sNew(String pDefName, int pNodeId, Object... pControls) void
        +nSet(int pNodeId, Object... pControls) void
        +encode(String pPath, Object... pArgs)$ byte[]
        +close() void
    }

    Instrument <|-- GamepadInstrument
    Instrument <|-- MicrophoneInstrument
    Instrument <|-- WebcamInstrument
    Instrument o-- ISynthCommand : BRIDGE (tiene un comando)

    GamepadInstrument --> IGamepadReader
    IGamepadReader <|.. JamepadReader
    MicrophoneInstrument --> IAudioSource
    MicrophoneInstrument *-- PitchDetector
    IAudioSource <|.. MicrophoneSource
    WebcamInstrument --> IFrameSource
    WebcamInstrument *-- HandTracker
    IFrameSource <|.. WebcamSource

    ISynthCommand <|.. SynthCommand
    SynthCommand <|-- PianoCommand
    SynthCommand <|-- BassCommand
    SynthCommand <|-- OrganCommand
    SynthCommand --> OscClient
    SynthCommand o-- "0..*" ISynthEventListener : avisa a
```

- `SynthCommand.execute` es un **Template Method**: valida → pone la hora → `send` (cada hijo) → avisa a los observadores.
- `ISynthEventListener` es el **Observador**: lo implementan `Recorder` y `MonitorPanel`.

## 3. Editor, almacenamiento y modelo

```mermaid
classDiagram
    direction TB

    class ISynthEventListener {
        <<interface>>
        +onSynthEvent(SynthEvent pEvent) void
    }
    class IPlaybackListener {
        <<interface>>
        +onPlaybackFinished(String pName) void
    }
    class IEditorView {
        <<interface>>
        +stateChanged(EditorState pState) void
        +recordingsChanged() void
        +showError(String pMessage) void
    }
    class IRecordingIndexer {
        <<interface>>
        +add(String pName, long pHeaderPosition) void
    }

    class EditorController {
        -EditorState state
        -RecordingHeader loaded
        -Map instruments
        +addInstrument(String pLabel, Instrument pInstrument) void
        +startInstruments() void
        +getInstrumentStatus() String
        +startRecording() void
        +stopRecording() void
        +saveRecording(String pName) void
        +discardRecording() void
        +load(String pName) void
        +play() void
        +stopPlayback() void
        +shutdown() void
        +formatEvent(SynthEvent pEvent)$ String
        +formatListEntry(ListEntry pEntry)$ String
    }

    class EditorState {
        <<enumeration>>
        IDLE
        RECORDING
        NAMING
        PLAYING
    }

    class Recorder {
        -State state
        -long headerPosition
        -long endPosition
        -long startTimeMs
        -StorageException writeFailure
        +start() void
        +stop() void
        +save(String pName) void
        +discard() void
        +onSynthEvent(SynthEvent pEvent) void
    }

    class Player {
        -Set~Integer~ heldOrganNotes
        -Thread thread
        +play(RecordingHeader pHeader) void
        +start(RecordingHeader pHeader) void
        +stop() void
        +setListener(IPlaybackListener pListener) void
    }

    class RecordingLibrary {
        +listCount() long
        +listEntry(long pIndex) ListEntry
        +load(String pName) RecordingHeader
    }

    class RecordingStorage {
        -File musicFile
        -File listFile
        +appendHeader(RecordingHeader pHeader) long
        +writeHeader(long pPosition, RecordingHeader pHeader) void
        +readHeader(long pPosition) RecordingHeader
        +appendEvent(SynthEvent pEvent) void
        +readEvent(long pPosition) SynthEvent
        +truncateMusic(long pLength) void
        +appendListEntry(ListEntry pEntry) void
        +listContainsName(String pName) boolean
        +readListEntry(long pIndex) ListEntry
        +isValidName(String pName)$ boolean
    }

    class RecordingIndex {
        -File indexFile
        -File[] tempFiles
        +add(String pName, long pHeaderPosition) void
        +find(String pName) Optional~IndexEntry~
        -sort() void
        -distribute(File pFirst, File pSecond) void
        -mergePass(int pInputs, int pOutputs, long pRunLength) long
        -mergeRuns(RunReader pA, RunReader pB, long pRunLength, DataOutputStream pOut)$ long
    }

    class SynthEvent {
        <<inmutable>>
        -long timeMs
        -SynthType synth
        -SynthAction action
        -int note
        -int durationMs
        -int third
        -float amp
        -float r1, r2, r3
        -float a1, a2, a3
        +piano(int pNote, int pDurationMs, int pThird)$ SynthEvent
        +bass(int pNote, int pDurationMs)$ SynthEvent
        +organOn(int pNote, float pAmp)$ SynthEvent
        +organOff(int pNote)$ SynthEvent
        +withTimeMs(long pTimeMs) SynthEvent
    }
    class SynthType {
        <<enumeration>>
        PIANO
        BASS
        ORGAN
    }
    class SynthAction {
        <<enumeration>>
        TRIGGER
        NOTE_ON
        NOTE_OFF
    }
    class RecordingHeader {
        -String name
        -long startPosition
        -long endPosition
    }
    class ListEntry {
        -String name
        -long createdAtMs
    }
    class IndexEntry {
        -String name
        -long headerPosition
    }

    ISynthEventListener <|.. Recorder
    IPlaybackListener <|.. EditorController
    IRecordingIndexer <|.. RecordingIndex

    EditorController --> Recorder
    EditorController --> Player
    EditorController --> RecordingLibrary
    EditorController --> IEditorView : avisa
    EditorController --> EditorState
    Player --> IPlaybackListener : avisa al terminar

    Recorder --> RecordingStorage
    Recorder --> IRecordingIndexer
    Player --> RecordingStorage
    RecordingLibrary --> RecordingStorage
    RecordingLibrary --> RecordingIndex

    RecordingStorage ..> SynthEvent
    RecordingStorage ..> RecordingHeader
    RecordingStorage ..> ListEntry
    RecordingIndex ..> IndexEntry
    SynthEvent --> SynthType
    SynthEvent --> SynthAction
```

- `Recorder` y `Player` reciben los mismos 3 comandos que los instrumentos (el `Player` los usa para reproducir sin instrumentos).
- `IRecordingIndexer` es el punto de unión entre la grabación (ft-5) y el índice (ft-6).

## 4. Ventana (Swing)

```mermaid
classDiagram
    direction LR

    class JFrame
    class JScrollPane
    class AbstractListModel~String~

    class EditorWindow {
        -EditorController controller
        -CountDownLatch closedLatch
        -JButton recordButton
        -JButton stopRecordingButton
        -JButton loadButton
        -JButton playButton
        -JButton stopPlaybackButton
        -JList~String~ list
        -JLabel statusBar
        +stateChanged(EditorState pState) void
        +recordingsChanged() void
        +showError(String pMessage) void
        -stopRecordingAndAskName() void
        -applyState() void
        -closeWindow() void
    }

    class MonitorPanel {
        -JTextArea text
        +onSynthEvent(SynthEvent pEvent) void
    }

    class RecordingListModel {
        -int size
        +getSize() int
        +getElementAt(int pIndex) String
        +nameAt(int pIndex) String
        +refresh() void
    }

    class IEditorView {
        <<interface>>
    }
    class ISynthEventListener {
        <<interface>>
    }
    class EditorController

    JFrame <|-- EditorWindow
    IEditorView <|.. EditorWindow
    JScrollPane <|-- MonitorPanel
    ISynthEventListener <|.. MonitorPanel
    AbstractListModel <|-- RecordingListModel

    EditorWindow *-- MonitorPanel
    EditorWindow *-- RecordingListModel
    EditorWindow --> EditorController
    RecordingListModel --> EditorController : lee la lista de disco
```

`EditorController` no conoce Swing (solo `IEditorView`), por eso se prueba sin abrir ventanas. Todo lo que toca la ventana pasa por `SwingUtilities.invokeLater`.

## 5. Excepciones propias

Todas son *checked* (heredan de `Exception` a través de `PowerBandException`).

```mermaid
classDiagram
    direction TB
    class Exception
    class PowerBandException
    class DeviceException
    class StorageException
    class SynthConnectionException
    class DuplicateRecordingNameException
    class RecordingNotFoundException

    Exception <|-- PowerBandException
    PowerBandException <|-- DeviceException
    PowerBandException <|-- StorageException
    PowerBandException <|-- SynthConnectionException
    PowerBandException <|-- DuplicateRecordingNameException
    PowerBandException <|-- RecordingNotFoundException
```

| Excepción | Cuándo |
|---|---|
| `DeviceException` | No hay control, micrófono o webcam, o no se pudo abrir. |
| `SynthConnectionException` | Puerto inválido, host que no existe o fallo al enviar por UDP. |
| `StorageException` | Error al leer o escribir los archivos (envuelve la `IOException`). |
| `DuplicateRecordingNameException` | El nombre ya está en `recordings.list`. |
| `RecordingNotFoundException` | La búsqueda binaria no encontró el nombre. |

## 6. Estados del editor

```mermaid
stateDiagram-v2
    [*] --> IDLE
    IDLE --> RECORDING : Grabar
    RECORDING --> NAMING : Detener grabación (apaga notas del órgano)
    NAMING --> NAMING : nombre inválido o repetido
    NAMING --> IDLE : Guardar con nombre válido
    NAMING --> IDLE : Cancelar (se descarta)
    IDLE --> IDLE : Cargar (búsqueda binaria)
    IDLE --> PLAYING : Reproducir (si hay algo cargado)
    PLAYING --> IDLE : termina sola o Detener reproducción
```

No se puede grabar mientras se reproduce (se grabaría la reproducción) ni reproducir mientras se graba.

## 7. Grabar

```mermaid
sequenceDiagram
    actor U as Usuario
    participant W as EditorWindow
    participant C as EditorController
    participant R as Recorder
    participant I as Instrumento
    participant CMD as Comando (Bridge)
    participant S as RecordingStorage
    participant X as RecordingIndex

    U->>W: Grabar
    W->>C: startRecording()
    C->>R: start()
    R->>S: appendHeader(encabezado provisional, fin = -1)

    loop mientras se toca
        I->>CMD: execute(SynthEvent)
        CMD->>CMD: envía por OSC a SuperCollider
        CMD-->>R: onSynthEvent(evento)
        R->>S: appendEvent(evento con tiempo relativo)
    end

    U->>W: Detener grabación
    W->>C: stopRecording()
    C->>R: stop()
    R->>CMD: organ.allNotesOff()
    CMD-->>R: onSynthEvent(NOTE_OFF) se graba también
    W->>U: pide el nombre
    U->>W: nombre
    W->>C: saveRecording(nombre)
    C->>R: save(nombre)
    R->>S: listContainsName(nombre) lee recordings.list en disco
    R->>S: writeHeader(posición, nombre, inicio, fin) con seek
    R->>S: appendListEntry(nombre, fecha)
    R->>X: add(nombre, posición del encabezado)
    X->>X: Balanced Merge Sort en disco
```

## 8. Cargar y reproducir

```mermaid
sequenceDiagram
    actor U as Usuario
    participant W as EditorWindow
    participant C as EditorController
    participant L as RecordingLibrary
    participant X as RecordingIndex
    participant S as RecordingStorage
    participant P as Player
    participant CMD as Comandos

    W->>C: recordingCount() y recordingAt(i)
    C->>L: lee recordings.list en orden cronológico
    U->>W: elige una grabación y Cargar
    W->>C: load(nombre)
    C->>L: load(nombre)
    L->>X: find(nombre) búsqueda binaria en disco
    X-->>L: IndexEntry(nombre, posición)
    L->>S: readHeader(posición)
    S-->>C: RecordingHeader(inicio, fin)

    U->>W: Reproducir
    W->>C: play()
    C->>P: start(encabezado) en su propio hilo
    loop de inicio a fin, de 64 en 64 bytes
        P->>S: readEvent(posición)
        P->>P: espera hasta inicio + tiempo grabado
        P->>CMD: execute(evento)
    end
    P->>CMD: apaga notas del órgano que quedaron encendidas
    P-->>C: onPlaybackFinished(nombre)
    C-->>W: stateChanged(IDLE)
```

## 9. Formato de los archivos binarios

Todos los registros de un mismo archivo miden lo mismo, así se puede saltar a cualquiera con `posición = número × tamaño`.

**`recordings.music`**: registros de **64 bytes**, uno tras otro. Cada grabación es un encabezado seguido de sus comandos.

| Encabezado | Bytes | | Comando | Bytes |
|---|---|---|---|---|
| tipo = 1 | 1 | | tipo = 2 | 1 |
| nombre (ASCII, relleno con espacios) | 32 | | tiempo relativo (ms) | 8 |
| posición de inicio | 8 | | synth (1 piano, 2 bajo, 3 órgano) | 1 |
| posición de fin | 8 | | acción (1 disparar, 2 encender, 3 apagar) | 1 |
| relleno | 15 | | nota | 4 |
| | | | duración (ms) | 4 |
| | | | tercera | 4 |
| | | | volumen | 4 |
| | | | r1, r2, r3 | 12 |
| | | | a1, a2, a3 | 12 |
| | | | relleno | 13 |
| **Total** | **64** | | **Total** | **64** |

**`recordings.list`**: registros de **40 bytes** (nombre 32 + fecha en ms 8), en orden cronológico.

**`recordings.index`**: registros de **40 bytes** (nombre 32 + posición del encabezado 8), ordenados por nombre.

```mermaid
flowchart LR
    subgraph MUS["recordings.music"]
        direction TB
        H1["0: Encabezado 'Prueba1'<br/>inicio 64, fin 320"]
        C1["64: Comando"]
        C2["128: Comando"]
        C3["192: Comando"]
        C4["256: Comando"]
        H2["320: Encabezado 'Jam'<br/>inicio 384, fin 512"]
        C5["384: Comando"]
        C6["448: Comando"]
        H1 --- C1 --- C2 --- C3 --- C4 --- H2 --- C5 --- C6
    end

    subgraph IDX["recordings.index (ordenado)"]
        direction TB
        I1["Jam → 320"]
        I2["Prueba1 → 0"]
        I1 --- I2
    end

    subgraph LST["recordings.list (cronológico)"]
        direction TB
        L1["Prueba1, 2026-11-23 17:50"]
        L2["Jam, 2026-11-23 18:05"]
        L1 --- L2
    end

    I1 -.-> H2
    I2 -.-> H1
```

Escritura: cada registro se arma en memoria con `DataOutputStream` sobre `ByteArrayOutputStream` (así mide exactamente 64 o 40 bytes) y se escribe de una vez con `RandomAccessFile`. La lectura secuencial de `recordings.list` usa `DataInputStream` sobre `BufferedInputStream` sobre `FileInputStream`.

## 10. Balanced Merge Sort del índice

Al guardar cada grabación, `RecordingIndex.add` agrega el registro al final de `recordings.index` (queda desordenado) y lo reordena **en disco** con 4 archivos temporales. En memoria solo están los 2 registros que se comparan.

```mermaid
flowchart TD
    A["add(nombre, posición):<br/>escribe el registro al final de recordings.index"] --> B{"¿hay 2 o más registros?"}
    B -- no --> Z["listo"]
    B -- sí --> C["REPARTIR: copia el índice alternando<br/>registro por registro en merge1 y merge2<br/>(tramos de 1)"]
    C --> D["MEZCLAR una pasada: toma un tramo de cada entrada,<br/>compara los 2 registros actuales y escribe el menor.<br/>Los tramos mezclados se alternan entre las 2 salidas"]
    D --> E["el largo del tramo se duplica<br/>las salidas pasan a ser entradas"]
    E --> F{"¿la segunda salida<br/>quedó vacía?"}
    F -- no --> D
    F -- sí --> G["todo quedó en un solo tramo ordenado:<br/>se copia sobre recordings.index"]
    G --> H["se borran merge1..merge4"]
```

Ejemplo con 4 nombres (`d, b, a, c`):

| Paso | Entrada 1 | Entrada 2 | Salida 1 | Salida 2 |
|---|---|---|---|---|
| Repartir (tramos de 1) | `d`, `a` | `b`, `c` | | |
| Pasada 1 (tramos de 1 → 2) | `d`, `a` | `b`, `c` | `b d` | `a c` |
| Pasada 2 (tramos de 2 → 4) | `b d` | `a c` | `a b c d` | *(vacía → terminó)* |

**Búsqueda binaria** (`RecordingIndex.find`): con `RandomAccessFile` lee solo el registro del medio (`seek(medio × 40)`), compara el nombre y descarta la mitad que no sirve, hasta encontrarlo o quedarse sin registros.
