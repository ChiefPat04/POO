---
id: ft-4
layer: backend
status: approved 
---

# Webcam → piano

## General description

- `WebcamInstrument` (extiende `Instrument`) mira la webcam y toca `classicPiano` con `SynthEvent.piano`: altura de la mano = nota, lado de la imagen = acorde mayor o menor, movimiento brusco = disparo.
- La captura va detrás de `IFrameSource`. `WebcamSource` es la real (librería Webcam Capture, ver ft-4 deploy); las pruebas usan una falsa con imágenes generadas.
- `HandTracker` es una clase aparte (como `PitchDetector` en ft-2): compara cada cuadro con el anterior y devuelve un `HandReading` con la posición y la cantidad de movimiento. Solo stdlib (`java.awt.image`).
- Un hilo propio lee cuadros sin parar.
- Actualiza el `PowerBandMain` provisional para iniciar también la webcam.

## Inputs

- Cuadros `BufferedImage` desde `IFrameSource.read()` (cualquier tamaño; `HandTracker` los reduce).
- `ISynthCommand` (normalmente `PianoCommand`) e `IFrameSource`, por constructor.

## Outputs

- `SynthEvent.piano(nota, CAM_DUR_MS, tercera, amp)` enviado con `play` al comando.
- `DeviceException` en `start()` si no hay webcam o no se puede abrir.
- `IllegalStateException` si se llama `start()` con el instrumento ya iniciado.

## Test criteria

- `HandTracker`:
  - El primer cuadro siempre da `HandReading.absent()` (no hay con qué comparar).
  - Dos cuadros iguales → ausente.
  - Un cambio muy pequeño (menos de `CAM_MIN_MOTION` de la cuadrícula) → ausente.
  - Un cambio de casi toda la imagen (más de `CAM_MAX_MOTION`, p. ej. cambio de luz) → ausente.
  - Un cuadrado que se mueve → presente; `x`, `y` = centro de los píxeles que cambiaron, normalizados 0..1, con `x` espejada si `CAM_MIRROR`.
  - Un cuadrado arriba da `y` menor que uno abajo.
  - Un movimiento más grande da `motion` mayor.
  - `reset()` olvida el cuadro anterior: el siguiente vuelve a dar ausente.
- `WebcamInstrument`:
  - `motion ≥ CAM_STRIKE_MOTION` con el instrumento armado → un `TRIGGER` de `PIANO`.
  - Mientras el movimiento sigue alto no se repite el disparo.
  - Se rearma cuando `motion < CAM_REARM_MOTION` o la mano desaparece; entonces otro golpe vuelve a disparar.
  - Dos golpes con menos de `CAM_MIN_STRIKE_INTERVAL_MS` entre sí dan un solo acorde, aunque el instrumento se haya rearmado en medio.
  - Con `motion` entre `CAM_REARM_MOTION` y `CAM_STRIKE_MOTION` ni dispara ni se rearma.
  - Nota según la altura: `y = 0` (arriba) → 72; `y = 1` (abajo) → 48. Bandas de igual alto, una por nota de Do mayor entre 48 y 72 (15 notas).
  - Toda nota está entre `PIANO_MIN_NOTE` (48) y `PIANO_MAX_NOTE` (72) y en la escala de Do mayor.
  - `x < CAM_SPLIT_X` → `THIRD_MAJOR`; `x ≥ CAM_SPLIT_X` → `THIRD_MINOR`.
  - `amp` = `motion × CAM_AMP_GAIN`, limitado a `CAM_MIN_AMP`..`CAM_MAX_AMP`. `durMs` = `CAM_DUR_MS`.
  - `start()` con una fuente que falla en `open()` lanza `DeviceException` y no arranca el hilo.
  - Si la fuente deja de dar cuadros (`null`), el hilo sigue vivo y vuelve a tocar cuando regresan.
  - `stop()` detiene el hilo y cierra la fuente; llamarlo dos veces o sin `start()` no falla.

## Assets

- `WebcamInstrument extends Instrument`:
  - `WebcamInstrument(ISynthCommand pCommand, IFrameSource pSource)`
  - `void start() throws DeviceException`
  - `void stop()`
  - `void process(HandReading pReading) throws SynthConnectionException` — aplica las reglas a una lectura; lo llama el hilo y también las pruebas.
  - `static int heightToNote(float pY)` — altura normalizada → nota MIDI de Do mayor entre 48 y 72.
- `IFrameSource`:
  - `void open() throws DeviceException`
  - `BufferedImage read()` — cuadro actual, o `null` si no hay. Nunca lanza.
  - `void close()`
- `WebcamSource implements IFrameSource` (Webcam Capture `0.3.12`, paquete `com.github.sarxos.webcam`):
  - `open`: `Webcam.getDefault()`; si es `null` → `DeviceException(ERR_NO_WEBCAM)`. `setViewSize(WebcamResolution.QVGA.getSize())` y `open()`; si devuelve `false` → `DeviceException(ERR_WEBCAM_OPEN)`.
  - `read`: si no está abierta → `null`; si no, `getImage()`.
  - `close`: `close()` de la cámara si está abierta.
- `HandTracker`:
  - `HandTracker(int pGridWidth, int pGridHeight)`
  - `HandReading track(BufferedImage pFrame)`
  - `void reset()`
- `HandReading` (modelo, inmutable, con getters): `HandReading(boolean pPresent, float pX, float pY, float pMotion)`; fábrica `HandReading.absent()`.
- Claves nuevas en `IConstants`:
  - Rango: `PIANO_MIN_NOTE` (48), `PIANO_MAX_NOTE` (72), `PIANO_ROOT_NOTE` (48, Do3), `CAM_MAX_STEP` (14: dos octavas de Do mayor).
  - Cuadrícula: `CAM_GRID_WIDTH` (64), `CAM_GRID_HEIGHT` (48), `CAM_PIXEL_THRESHOLD` (30, en 0..255), `CAM_MIRROR` (true), `CAM_PIXEL_CENTER` (0.5f).
  - Movimiento (fracción de la cuadrícula): `CAM_MIN_MOTION` (0.01f), `CAM_REARM_MOTION` (0.02f), `CAM_STRIKE_MOTION` (0.06f), `CAM_MAX_MOTION` (0.5f).
  - Disparo: `CAM_SPLIT_X` (0.5f), `CAM_DUR_MS` (1000), `CAM_AMP_GAIN` (4f), `CAM_MIN_AMP` (0.15f), `CAM_MAX_AMP` (0.6f), `CAM_MIN_STRIKE_INTERVAL_MS` (250).
  - Hilo: `CAM_POLL_INTERVAL_MS` (33), `CAM_RETRY_WAIT_MS` (500), `CAM_STOP_TIMEOUT_MS` (1000), `CAM_THREAD_NAME`.
  - Mensajes: `ERR_NO_WEBCAM`, `ERR_WEBCAM_OPEN`, `ERR_WEBCAM_ALREADY_STARTED`, `ERR_NULL_FRAME_SOURCE`, `ERR_WEBCAM_PLAY`.
- Los umbrales son valores iniciales: hay que ajustarlos con la webcam, la luz y la distancia reales.

## Implementation guidelines

- Archivos:
  - `src/powerband/instrument/WebcamInstrument.java`, `IFrameSource.java`, `WebcamSource.java`, `HandTracker.java`
  - `src/powerband/model/HandReading.java`
  - `src/powerband/PowerBandMain.java`
  - `src/powerband/util/IConstants.java`
- `HandTracker.track`:
  1. Reduce el cuadro a una imagen `TYPE_BYTE_GRAY` de `CAM_GRID_WIDTH × CAM_GRID_HEIGHT` con `Graphics2D.drawImage` (escalar y pasar a gris en un paso) y saca los valores con `getRaster().getSample`.
  2. Sin cuadro anterior → guarda este y devuelve `absent()`.
  3. Un píxel cambió si `|actual − anterior| ≥ CAM_PIXEL_THRESHOLD`. `motion` = píxeles cambiados / total.
  4. Guarda el cuadro actual como anterior.
  5. `motion < CAM_MIN_MOTION` o `motion > CAM_MAX_MOTION` → `absent()`.
  6. Si no: `x = (promedio de columnas + CAM_PIXEL_CENTER) / ancho`, `y = (promedio de filas + CAM_PIXEL_CENTER) / alto`; si `CAM_MIRROR`, `x = 1 − x`. Devuelve `HandReading(true, x, y, motion)`.
- La diferencia entre cuadros solo ve lo que se mueve: el cuerpo quieto no cuenta y el centro cae donde se movió la mano. Limitación: si se mueve todo el cuerpo, el centro se corre.
- `heightToNote`: limita `y` a 0..1; `paso = min(CAM_MAX_STEP, (int) ((1 − y) × (CAM_MAX_STEP + 1)))`; `nota = PIANO_ROOT_NOTE + OCTAVE_SEMITONES × floorDiv(paso, 7) + C_MAJOR_SCALE[floorMod(paso, 7)]`; al final se limita a `PIANO_MIN_NOTE`..`PIANO_MAX_NOTE`. El 7 sale de `C_MAJOR_SCALE.size()`.
- `WebcamInstrument.process` (`synchronized`), con una bandera `armed` (empieza en `true`):
  1. Ausente o `motion < CAM_REARM_MOTION` → `armed = true` y nada más.
  2. `motion ≥ CAM_STRIKE_MOTION`, `armed` y pasaron al menos `CAM_MIN_STRIKE_INTERVAL_MS` desde el último disparo → `armed = false`, se guarda la hora del disparo y `play(SynthEvent.piano(heightToNote(y), CAM_DUR_MS, tercera, amp))`.
  3. Cualquier otro caso → nada (histéresis, igual que el stick en ft-3).
  - El tiempo mínimo entre disparos evita que un solo golpe suene dos veces: la cámara a veces entrega el mismo cuadro dos veces, eso da movimiento 0 y rearmaba el instrumento en medio del golpe.
- Hilo: daemon, con nombre `CAM_THREAD_NAME`. Ciclo: `read()`; si es `null`, `tracker.reset()`, `process(HandReading.absent())`, espera `CAM_RETRY_WAIT_MS` y sigue; si no, `process(tracker.track(cuadro))` y espera `CAM_POLL_INTERVAL_MS`.
- El hilo no puede lanzar `SynthConnectionException`: la reporta en `System.err` con `ERR_WEBCAM_PLAY` y sigue. Nunca `catch` vacío.
- `start()` y `stop()` usan un lock propio, distinto del de `process`, para que `stop()` no espere al hilo (igual que `MicrophoneInstrument`).
- `stop()`: baja la bandera `volatile`, interrumpe, `join(CAM_STOP_TIMEOUT_MS)` y luego `close()` de la fuente. El piano termina solo: no hay nota que apagar.
- `WebcamSource`: `WebcamException`, `IllegalStateException` y `UnsatisfiedLinkError` de la librería se envuelven en `DeviceException` con la causa. `read()` atrapa `WebcamException` y devuelve `null`.
- En Windows, si el permiso de cámara está apagado, `Webcam.getDefault()` puede devolver `null` o la apertura falla: ambos terminan en `DeviceException`.
- `PowerBandMain` (provisional, lo reemplaza ft-8): agrega `new WebcamInstrument(piano, new WebcamSource())` a la lista de instrumentos; el resto queda como en ft-2.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.
- Fuera de alcance: elegir entre varias cámaras, mostrar el video, seguimiento de la mano por color o con IA, cambiar `durMs` con la mano, ventana del editor (ft-8).

## Business workflow

1. `start()`: `source.open()`; si falla, `DeviceException`. Si no, arranca el hilo.
2. El hilo lee un cuadro cada `CAM_POLL_INTERVAL_MS`.
3. `HandTracker` lo compara con el anterior y da posición y movimiento.
4. `process`: movimiento brusco con el instrumento armado → nota según la altura, tercera según el lado, `amp` según el movimiento, y `play`. Se desarma hasta que el movimiento baje.
5. Si la cámara deja de dar cuadros, el hilo espera `CAM_RETRY_WAIT_MS` y reintenta.
6. `stop()`: detiene el hilo y cierra la cámara.
