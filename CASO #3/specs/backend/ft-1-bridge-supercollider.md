---
id: ft-1
layer: backend
status: approved
---

# Contratos Bridge y conexión SuperCollider

## General description

- Crea las dos jerarquías del Bridge: `Instrument` (abstracción, sin dispositivo real) e `ISynthCommand` (implementación) con `PianoCommand`, `BassCommand` y `OrganCommand`.
- `OscClient` codifica y envía mensajes OSC `/s_new` y `/n_set` por UDP a `scsynth`, basado en `recursos/supercollider/BandClient.java`.
- Cada comando ejecutado crea un `SynthEvent` con la hora de envío y lo notifica a los `ISynthEventListener` registrados (observador para el editor).
- Agrega `PowerBandException` y `SynthConnectionException`.

## Inputs

- `SynthEvent` (desde un `Instrument` o, en ft-7, desde `Player`): synth, acción, nota MIDI, `durMs`, tercera, `amp`, `r1..r3`, `a1..a3`. El tiempo lo pone el comando.
- Host y puerto de `scsynth`: `IConstants.SC_HOST` (`127.0.0.1`), `IConstants.SC_PORT` (`57110`).

## Outputs

- Datagramas UDP OSC hacia `scsynth`:
  - Piano: `/s_new "classicPiano" <nodeId> 0 1 "note" i "durMs" i "third" i "amp" f`
  - Bajo: `/s_new "stringBass" <nodeId> 0 1 "note" i "durMs" i "amp" f`
  - Órgano noteOn: `/s_new "organ70s" <nodeId> 0 1 "freq" f "amp" f "r1" f "r2" f "r3" f "a1" f "a2" f "a3" f`
  - Órgano noteOff: `/n_set <nodeId> "gate" 0`
- `SynthEvent` notificado a cada `ISynthEventListener.onSynthEvent(SynthEvent pEvent)`, con `timeMs` = `System.currentTimeMillis()` al enviar (tiempo **absoluto**, ver nota abajo).
- `SynthConnectionException` cuando el puerto está fuera de rango, el socket no se puede crear, el host no resuelve o falla el envío.
- `IllegalArgumentException` cuando un comando recibe un evento que no le corresponde (synth o acción equivocados).

### Nota sobre el tiempo del evento

- En ft-1, `timeMs` es la hora absoluta de envío (`System.currentTimeMillis()`).
- El instante **relativo** que pide `CLAUDE.md` para `SynthEvent` lo calcula el grabador (ft-5): `timeMs` del evento − hora de inicio de la grabación, usando `withTimeMs(long pTimeMs)`. Ese valor relativo es el que se escribe en `recordings.music`.
- `Player` (ft-7) usa los tiempos relativos solo para esperar entre comandos; al ejecutar, el comando vuelve a estampar la hora absoluta.

### Nota sobre la conexión UDP

- UDP no tiene conexión: `OscClient` no puede detectar que `scsynth` esté apagado. `SynthConnectionException` solo cubre errores locales (crear el socket, resolver el host, enviar el datagrama). Un servidor apagado simplemente no suena.

## Test criteria

- `OscClient.encode` produce bytes idénticos al algoritmo de `BandClient.send`: strings ASCII terminados en null y rellenados a múltiplo de 4, type tag `,` + `i`/`f`/`s`, enteros big-endian, floats como `Float.floatToIntBits`.
- `encode` con un tipo no soportado lanza `IllegalArgumentException`.
- Cada comando envía exactamente los mensajes de **Outputs** (path, defName, args y orden).
- Cada comando lanza `IllegalArgumentException` si recibe un evento de otro synth o con una acción que no soporta (piano y bajo: solo `TRIGGER`; órgano: solo `NOTE_ON`/`NOTE_OFF`), y en ese caso no envía nada ni notifica.
- Los `nodeId` empiezan en `IConstants.FIRST_NODE_ID` (10000) y nunca se repiten, aun con varios hilos.
- `OrganCommand`: `noteOn` sobre una nota ya encendida envía primero `/n_set gate 0` al nodo viejo; `noteOff` de una nota no encendida no envía nada ni notifica.
- `OrganCommand`: con varios hilos haciendo `noteOn`/`noteOff` sobre la misma nota, al final no queda ningún nodo encendido sin registrar en el mapa.
- `freq` del órgano = `440 * 2^((nota - 69) / 12)` como `float`.
- Las fábricas de `SynthEvent` producen eventos con el synth y la acción correctos y los valores por defecto de `IConstants` donde no se pasan.
- Todo listener registrado recibe un evento por mensaje enviado; un listener removido ya no recibe.
- `Instrument.setCommand` cambia el synth destino sin cambiar la subclase de `Instrument`.

## Assets

- Contratos:
  - `ISynthCommand`: `void execute(SynthEvent pEvent) throws SynthConnectionException`, `void addListener(ISynthEventListener pListener)`, `void removeListener(ISynthEventListener pListener)`.
  - `ISynthEventListener`: `void onSynthEvent(SynthEvent pEvent)`.
  - `Instrument` (abstracta): `Instrument(ISynthCommand pCommand)`, `setCommand(ISynthCommand pCommand)`, `getCommand()`, `protected void play(SynthEvent pEvent) throws SynthConnectionException`, `public abstract void start() throws PowerBandException`, `public abstract void stop()`.
  - `OscClient implements AutoCloseable`:
    - `OscClient(String pHost, int pPort) throws SynthConnectionException`
    - `int nextNodeId()`
    - `void sNew(String pDefName, int pNodeId, Object... pControls) throws SynthConnectionException`
    - `void nSet(int pNodeId, Object... pControls) throws SynthConnectionException`
    - `public static byte[] encode(String pPath, Object... pArgs)`
    - `void close()` (sin `throws`; solo cierra el socket)
- Modelo:
  - `SynthEvent` inmutable: `timeMs`, `SynthType synth`, `SynthAction action`, `note`, `durationMs`, `third`, `amp`, `r1..r3`, `a1..a3`, con getters y `withTimeMs(long pTimeMs)`.
  - Constructor `private`; se crea con fábricas estáticas que validan synth/acción:
    - `SynthEvent.piano(int pNote, int pDurationMs, int pThird, float pAmp)` y `SynthEvent.piano(int pNote, int pDurationMs, int pThird)` (amp por defecto).
    - `SynthEvent.bass(int pNote, int pDurationMs, float pAmp)` y `SynthEvent.bass(int pNote, int pDurationMs)`.
    - `SynthEvent.organOn(int pNote, float pAmp, float[] pRatios, float[] pAmps)` y `SynthEvent.organOn(int pNote)` (ratios, amplitudes y amp por defecto). `pRatios` y `pAmps` deben medir `ORGAN_PARTIALS` (3); si no, `IllegalArgumentException`.
    - `SynthEvent.organOff(int pNote)`.
    - Los campos que no aplican a un synth quedan en `IConstants.UNUSED_INT` / `UNUSED_FLOAT` (0).
  - Enums `SynthType { PIANO, BASS, ORGAN }` y `SynthAction { TRIGGER, NOTE_ON, NOTE_OFF }`.
- Claves nuevas en `IConstants`:
  - Conexión: `SC_HOST`, `SC_PORT`, `PORT_MIN` (0), `PORT_MAX` (65535), `FIRST_NODE_ID` (10000), `ADD_TO_HEAD` (0), `DEFAULT_GROUP` (1).
  - OSC: `OSC_S_NEW` (`/s_new`), `OSC_N_SET` (`/n_set`), `OSC_ALIGNMENT` (4), `OSC_TYPE_TAG_PREFIX` (`','`), `OSC_TAG_INT` (`'i'`), `OSC_TAG_FLOAT` (`'f'`), `OSC_TAG_STRING` (`'s'`), `OSC_NULL_BYTE` (0), `BYTE_SHIFT_24` (24), `BYTE_SHIFT_16` (16), `BYTE_SHIFT_8` (8), `S_NEW_FIXED_ARGS` (4: defName, nodeId, addAction, target).
  - Synths: `SYNTH_PIANO` (`classicPiano`), `SYNTH_BASS` (`stringBass`), `SYNTH_ORGAN` (`organ70s`).
  - Parámetros: `PARAM_NOTE`, `PARAM_DUR_MS`, `PARAM_THIRD`, `PARAM_AMP`, `PARAM_FREQ`, `PARAM_R1..PARAM_R3`, `PARAM_A1..PARAM_A3`, `PARAM_GATE`.
  - Valores: `GATE_OFF` (0), `THIRD_MAJOR` (4), `THIRD_MINOR` (3), `MIDI_A4` (69), `FREQ_A4` (440.0), `SEMITONES_PER_OCTAVE` (12.0), `FREQ_BASE` (2.0), `ORGAN_PARTIALS` (3), `UNUSED_INT` (0), `UNUSED_FLOAT` (0f).
  - Por defecto (iguales a `myband.sc`): `DEFAULT_PIANO_AMP` (0.3f), `DEFAULT_BASS_AMP` (0.5f), `DEFAULT_ORGAN_AMP` (0.3f), `DEFAULT_ORGAN_RATIOS` (`1, 2, 3`), `DEFAULT_ORGAN_AMPS` (`1, 0.5, 0.25`).
  - Mensajes de error de las excepciones también van en `IConstants`.

## Implementation guidelines

- Archivos:
  - `src/powerband/instrument/Instrument.java`
  - `src/powerband/command/ISynthCommand.java`, `SynthCommand.java` (abstracta: lista de listeners y notificación), `PianoCommand.java`, `BassCommand.java`, `OrganCommand.java`, `ISynthEventListener.java`
  - `src/powerband/sc/OscClient.java`
  - `src/powerband/model/SynthEvent.java`, `SynthType.java`, `SynthAction.java`
  - `src/powerband/exception/PowerBandException.java` (checked), `SynthConnectionException.java` (extiende `PowerBandException`)
  - `src/powerband/util/IConstants.java`
- Java 17 (`maven.compiler.release`), solo stdlib (`DatagramSocket`, `ByteArrayOutputStream`). Sin dependencias nuevas.
- `SynthCommand.execute` es template method:
  1. `protected abstract void validate(SynthEvent pEvent)` lanza `IllegalArgumentException` si el evento no corresponde.
  2. Estampa el tiempo con `withTimeMs`.
  3. Llama `protected abstract boolean send(SynthEvent pEvent) throws SynthConnectionException`, que devuelve si se envió algo.
  4. Notifica a los listeners solo si `send` devolvió `true`.
- El constructor de `OscClient` valida el puerto contra `PORT_MIN`/`PORT_MAX` antes de abrir el socket y lanza `SynthConnectionException` si está fuera de rango.
- Los comandos reciben el `OscClient` por constructor; nunca lo crean.
- Hilos: los instrumentos llaman desde hilos propios.
  - `nextNodeId` con `AtomicInteger`.
  - Listeners en `CopyOnWriteArrayList`.
  - `OrganCommand`: el mapa nota→nodo es un `HashMap` privado y **`noteOn`/`noteOff` son `synchronized`** (sobre el comando), porque el reinicio (buscar nodo viejo, enviar `gate 0`, registrar el nuevo) debe ser atómico. No basta con `Collections.synchronizedMap`.
- `IOException` del socket se envuelve en `SynthConnectionException`; nunca `catch` vacío (el `close` de `BandClient` no se copia así).
- `OscClient.close()` solo cierra el socket. No apaga notas del órgano: apagar las notas encendidas al cerrar la app o al detener una grabación queda para ft-5 (`allNotesOff`). Hasta entonces, cerrar la app con una nota del órgano sonando la deja pegada en SuperCollider.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- `BandClient.java` es solo referencia; no se compila ni se mueve.
- Fuera de alcance: dispositivos reales (ft-2..ft-4), `allNotesOff` para el grabador (ft-5), cálculo del tiempo relativo (ft-5), `DeviceException`/`StorageException` (las agrega la feature que las use), validación de rangos de nota por instrumento (ft-2..ft-4).

## Business workflow

1. Se construye `OscClient(SC_HOST, SC_PORT)`; si falla, `SynthConnectionException`.
2. Se crean `PianoCommand`, `BassCommand`, `OrganCommand` con ese `OscClient`.
3. El editor registra su `ISynthEventListener` en cada comando.
4. Una subclase de `Instrument` arma un `SynthEvent` con una fábrica (`SynthEvent.piano(...)`, etc.) y llama `play`, que delega en su `ISynthCommand`.
5. El comando valida el evento, estampa `timeMs`, pide un `nodeId`, envía `/s_new` o `/n_set` y notifica el evento a los listeners.
6. Al cerrar la app se cierra `OscClient` con try-with-resources.
