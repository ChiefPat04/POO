---
id: ft-3
layer: backend
status: approved
---

# Control PS5 → bajo

## General description

- `GamepadInstrument` (extiende `Instrument`) lee un control DualSense y toca `stringBass` con `SynthEvent.bass`.
- La lectura del dispositivo va detrás de `IGamepadReader`, que devuelve una foto del estado (`GamepadState`). `JamepadReader` es la implementación real con Jamepad; las pruebas usan una falsa.
- Un hilo propio consulta el estado cada `GAMEPAD_POLL_INTERVAL_MS` y traduce los cambios en notas.
- Agrega `DeviceException` y actualiza `PowerBandMain` de forma provisional para probar con SuperCollider.

## Inputs

- `GamepadState` desde `IGamepadReader.read()`: equis, círculo, cuadrado, triángulo, L1, R1 (`boolean`), stick izquierdo horizontal (`float`, -1 a 1), R2 (`float`, 0 a 1), conectado (`boolean`).
- `ISynthCommand` (normalmente `BassCommand`) e `IGamepadReader`, por constructor.

## Outputs

- `SynthEvent.bass(nota, durMs)` (amp por defecto) enviado con `play` al comando.
- `DeviceException` en `start()` si la librería no inicializa o no hay control conectado.
- `IllegalStateException` si se llama `start()` con el instrumento ya iniciado.

## Test criteria

- Solo dispara cuando un botón pasa de suelto a presionado: una nota por pulsación; mantenerlo presionado no repite; soltar y presionar vuelve a disparar.
- Con la base inicial: equis = 36, círculo = 38, cuadrado = 40, triángulo = 41.
- Varios botones presionados en la misma lectura disparan una nota cada uno.
- Stick a la derecha (≥ `GAMEPAD_STICK_PUSH_THRESHOLD`) sube la base un paso de la escala; a la izquierda la baja. Un paso por empujón: no vuelve a moverse hasta que el stick regrese a |x| ≤ `GAMEPAD_STICK_RELEASE_THRESHOLD`.
- R1 sube la base una octava (7 pasos) y L1 la baja; un cambio por pulsación.
- Un cambio de base que la saque de `GAMEPAD_MIN_STEP`..`GAMEPAD_MAX_STEP` se ignora (la base no cambia).
- La nota final siempre queda entre `BASS_MIN_NOTE` (28) y `BASS_MAX_NOTE` (55): con la base en el máximo, círculo, cuadrado y triángulo dan 55.
- Todas las notas pertenecen a la escala de Do mayor.
- R2 en 0 → `GAMEPAD_MIN_DUR_MS`; en 1 → `GAMEPAD_MAX_DUR_MS`; en medio, interpolación lineal redondeada.
- En una misma lectura primero se aplican stick, L1 y R1, y después se disparan los botones.
- Una lectura con `connected = false` se ignora: no dispara ni cambia la base.
- `start()` con un lector que falla en `open()` lanza `DeviceException` y no arranca el hilo.
- Si el control se desconecta, el hilo sigue vivo y vuelve a tocar cuando se reconecta.
- `stop()` detiene el hilo y llama `close()` del lector; llamarlo dos veces o sin `start()` no falla.

## Assets

- `GamepadInstrument extends Instrument`:
  - `GamepadInstrument(ISynthCommand pCommand, IGamepadReader pReader)`
  - `void start() throws DeviceException`
  - `void stop()`
  - `void process(GamepadState pState) throws SynthConnectionException` — traduce una lectura; lo llama el hilo y también las pruebas.
  - `static int stepToNote(int pStep)` — paso de la escala → nota MIDI, ya limitada al rango del bajo.
- `IGamepadReader`:
  - `void open() throws DeviceException` — inicializa; falla si no hay control.
  - `GamepadState read()` — nunca lanza; si no hay control devuelve `GamepadState.disconnected()`.
  - `void close()`
- `JamepadReader implements IGamepadReader` (Jamepad `2.30.0.0`, paquete `com.studiohartman.jamepad`):
  - `open`: `new ControllerManager()`, `initSDLGamepad()`, `update()`; si `getNumControllers()` es 0, `quitSDLGamepad()` y `DeviceException`.
  - `read`: `getState(GAMEPAD_INDEX)` → `a` = equis, `b` = círculo, `x` = cuadrado, `y` = triángulo, `lb` = L1, `rb` = R1, `leftStickX`, `rightTrigger`, `isConnected`.
  - `close`: `quitSDLGamepad()`.
- `GamepadState` (modelo, inmutable, con getters): constructor con los 9 valores; fábricas `GamepadState.disconnected()` y `GamepadState.idle()` (conectado, nada presionado).
- `DeviceException extends PowerBandException` (constructores mensaje y mensaje + causa).
- Claves nuevas en `IConstants`:
  - Rango y escala: `BASS_MIN_NOTE` (28), `BASS_MAX_NOTE` (55), `C_MAJOR_SCALE` (`List.of(0, 2, 4, 5, 7, 9, 11)`), `OCTAVE_SEMITONES` (12).
  - Base: `GAMEPAD_ROOT_NOTE` (36, Do2), `GAMEPAD_INITIAL_STEP` (0), `GAMEPAD_MIN_STEP` (-5, Mi1 = 28), `GAMEPAD_MAX_STEP` (11, Sol3 = 55), `GAMEPAD_STICK_STEP` (1), `GAMEPAD_OCTAVE_STEPS` (7).
  - Botones: `GAMEPAD_STEP_CROSS` (0), `GAMEPAD_STEP_CIRCLE` (1), `GAMEPAD_STEP_SQUARE` (2), `GAMEPAD_STEP_TRIANGLE` (3).
  - Stick: `GAMEPAD_STICK_PUSH_THRESHOLD` (0.6f), `GAMEPAD_STICK_RELEASE_THRESHOLD` (0.3f).
  - Duración: `GAMEPAD_MIN_DUR_MS` (300), `GAMEPAD_MAX_DUR_MS` (2000).
  - Hilo: `GAMEPAD_POLL_INTERVAL_MS` (10), `GAMEPAD_RECONNECT_WAIT_MS` (500), `GAMEPAD_STOP_TIMEOUT_MS` (1000), `GAMEPAD_THREAD_NAME`, `GAMEPAD_INDEX` (0), `NO_GAMEPADS` (0).
  - Mensajes: `ERR_NO_GAMEPAD`, `ERR_GAMEPAD_INIT`, `ERR_GAMEPAD_ALREADY_STARTED`, `ERR_NULL_READER`, `ERR_GAMEPAD_PLAY`, `MSG_PRESS_ENTER_TO_EXIT`.

## Implementation guidelines

- Archivos:
  - `src/powerband/instrument/GamepadInstrument.java`, `IGamepadReader.java`, `JamepadReader.java`
  - `src/powerband/model/GamepadState.java`
  - `src/powerband/exception/DeviceException.java`
  - `src/powerband/PowerBandMain.java`
  - `src/powerband/util/IConstants.java`
- La base es un solo entero `baseStep`: pasos de la escala contados desde `GAMEPAD_ROOT_NOTE`. El stick suma o resta `GAMEPAD_STICK_STEP`; L1/R1 suman o restan `GAMEPAD_OCTAVE_STEPS`. No hay un campo de octava aparte.
- `stepToNote`: `octava = Math.floorDiv(paso, 7)`, `grado = Math.floorMod(paso, 7)`, `nota = GAMEPAD_ROOT_NOTE + OCTAVE_SEMITONES * octava + C_MAJOR_SCALE[grado]`, y al final se limita a `BASS_MIN_NOTE`..`BASS_MAX_NOTE`. El 7 sale de `C_MAJOR_SCALE.size()`.
- Detección de flanco: `process` guarda la lectura anterior y compara botón por botón. El stick usa una bandera "armado" con histéresis (los dos umbrales) para que el ruido cerca del umbral no dé pasos dobles.
- `process` es `synchronized`.
- Hilo: daemon, con nombre `GAMEPAD_THREAD_NAME`. Ciclo: `read()`; si no está conectado, espera `GAMEPAD_RECONNECT_WAIT_MS` y sigue; si está conectado, `process` y espera `GAMEPAD_POLL_INTERVAL_MS`.
- El hilo no puede lanzar `SynthConnectionException`: la reporta en `System.err` con `ERR_GAMEPAD_PLAY` y sigue leyendo. Nunca `catch` vacío. Mostrarla en la ventana queda para ft-8.
- `stop()`: baja la bandera `volatile`, interrumpe el hilo, hace `join(GAMEPAD_STOP_TIMEOUT_MS)` y luego `close()` del lector. Si `join` se interrumpe, restaura el flag de interrupción.
- `JamepadReader`: `IllegalStateException` y `UnsatisfiedLinkError` de Jamepad se envuelven en `DeviceException` con la causa. `read()` atrapa `IllegalStateException` y devuelve `disconnected()`.
- Riesgo a verificar con el control real: `open()` corre en el hilo que llama `start()` y `read()` en el hilo del instrumento. Si SDL exige el mismo hilo en Windows, mover `open()` al hilo del instrumento y pasar el resultado a `start()` con un `CountDownLatch`.
- `PowerBandMain` (provisional, lo reemplaza ft-8):
  1. `OscClient(SC_HOST, SC_PORT)` en try-with-resources.
  2. Crea `PianoCommand`, `BassCommand`, `OrganCommand`.
  3. Crea `GamepadInstrument(bass, new JamepadReader())` y llama `start()`.
  4. Imprime `MSG_PRESS_ENTER_TO_EXIT` y espera una línea en `System.in`.
  5. En `finally`: `stop()` y `organ.allNotesOff()`.
  6. `PowerBandException` se reporta en `System.err`.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: vibración y luces del control, más de un control, elegir otra escala, grabación (ft-5 ya escucha los comandos), ventana del editor (ft-8).

## Business workflow

1. `start()`: `reader.open()`; si falla, `DeviceException`. Si no, arranca el hilo.
2. El hilo lee el estado cada `GAMEPAD_POLL_INTERVAL_MS`.
3. `process`: si el stick dio un empujón, o L1/R1 pasaron a presionado, mueve `baseStep` (si queda dentro del rango).
4. `process`: por cada botón que pasó de suelto a presionado, calcula `stepToNote(baseStep + paso del botón)` y `durMs` desde R2, y llama `play(SynthEvent.bass(nota, durMs))`.
5. Si el control se desconecta, el hilo espera `GAMEPAD_RECONNECT_WAIT_MS` y vuelve a leer hasta que regrese.
6. `stop()`: detiene el hilo y libera la librería.
