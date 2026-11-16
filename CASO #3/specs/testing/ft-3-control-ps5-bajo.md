---
id: ft-3
layer: testing
status: draft
---

# Control PS5 → bajo

## General description

- Pruebas JUnit 5 de la lógica de `GamepadInstrument`: mapeo de botones a notas, detección de flanco, stick, octavas, duración, límites y ciclo de vida del hilo.
- Usan un lector falso y un comando que guarda eventos. Sin control, sin Jamepad cargado y sin SuperCollider.
- `JamepadReader` no tiene prueba automática (necesita el control); se prueba a mano con `PowerBandMain`.

## Inputs

- `GamepadState` armados en cada prueba.
- `FakeGamepadReader` como `IGamepadReader`.
- `RecordingCommand` (ya existe) como `ISynthCommand`.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `GamepadInstrumentTest` — mapeo, llamando `process` directo:
  - `pressingCrossPlaysRootNote`: 36, synth `BASS`, acción `TRIGGER`, amp `DEFAULT_BASS_AMP`.
  - `eachButtonPlaysItsScaleDegree`: equis 36, círculo 38, cuadrado 40, triángulo 41.
  - `heldButtonPlaysOnlyOnce`: la misma lectura presionada 3 veces → 1 evento.
  - `releasingAndPressingAgainPlaysAgain`.
  - `buttonsPressedTogetherPlayOneNoteEach`.
  - `stickRightRaisesBaseOneStepPerPush`: tras un empujón, equis = 38.
  - `stickHeldDoesNotKeepMovingBase`.
  - `stickMustReturnBelowReleaseThresholdBeforeNextPush`: 0.7 → 0.5 → 0.7 da un solo paso; 0.7 → 0.1 → 0.7 da dos.
  - `stickBelowPushThresholdDoesNothing`.
  - `stickLeftLowersBase`: equis = 35.
  - `r1RaisesBaseOneOctave`: equis = 48.
  - `l1LowersBaseOneOctave`: tras subir una octava, L1 regresa a 36.
  - `heldShoulderChangesOctaveOnlyOnce`.
  - `baseChangeOutsideRangeIsIgnored`: L1 desde la base inicial no cambia nada; dos R1 seguidos suben solo una octava.
  - `noteIsClampedToBassRange`: con la base en `GAMEPAD_MAX_STEP`, triángulo = 55.
  - `everyReachableNoteIsInCMajorAndInRange`: recorre de `GAMEPAD_MIN_STEP` a `GAMEPAD_MAX_STEP` con los 4 botones.
  - `triggerMapsDurationBetweenMinAndMax`: 0 → min, 1 → max, 0.5 → punto medio.
  - `baseChangesApplyBeforeButtonsInSameRead`: R1 y equis en la misma lectura → 48.
  - `disconnectedStateIsIgnored`.
  - `stepToNoteMapsLimits`: `GAMEPAD_MIN_STEP` → 28, 0 → 36, 7 → 48, `GAMEPAD_MAX_STEP` → 55.
- `GamepadInstrumentTest` — ciclo de vida, con el hilo real:
  - `startThrowsDeviceExceptionWhenReaderCannotOpen`: no hay lecturas después.
  - `startTwiceThrowsIllegalStateException`.
  - `pollingThreadPlaysNoteWhenButtonIsPressed`: se cambia el estado del lector falso y el evento llega antes de un timeout.
  - `threadSurvivesDisconnectionAndPlaysAfterReconnect`.
  - `stopClosesReaderAndStopsPolling`: tras `stop`, el contador de lecturas ya no crece.
  - `stopWithoutStartDoesNotFail`.
- Ninguna prueba depende del control ni de `scsynth`.

## Assets

- `src/test/powerband/instrument/helpers/FakeGamepadReader.java`: `IGamepadReader` con `setState(GamepadState pState)` (campo `volatile`), `setFailOnOpen(boolean pFail)`, `getReadCount()`, `isClosed()`.
- Reutiliza `src/test/powerband/instrument/helpers/RecordingCommand.java`. Su lista de eventos se lee desde otro hilo en las pruebas de ciclo de vida: si no es segura para hilos, cambiarla a `CopyOnWriteArrayList`.

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; fuentes en `src/test/`.
- Archivo: `src/test/powerband/instrument/GamepadInstrumentTest.java`.
- Clases `<ClassName>Test`; métodos que describen comportamiento.
- Notas base, pasos, umbrales y duraciones desde `IConstants`; las notas esperadas (36, 38, 40, 41, 48, 55) sí van escritas en la prueba para verificar el mapeo.
- Un método auxiliar privado arma `GamepadState` a partir de `GamepadState.idle()` para no repetir los 9 argumentos.
- Las pruebas con hilo esperan con un ciclo corto y un timeout máximo, no con un `sleep` fijo; siempre llaman `stop()` en `finally`.
- La prueba de reconexión tarda al menos `GAMEPAD_RECONNECT_WAIT_MS`.

## Business workflow

1. Crear `RecordingCommand`, `FakeGamepadReader` y `GamepadInstrument`.
2. Mapeo: llamar `process` con una secuencia de lecturas y comparar los eventos guardados.
3. Ciclo de vida: `start()`, cambiar el estado del lector falso, esperar el evento, `stop()`.
4. Verificar nota, `durMs`, cantidad de eventos y estado del lector.
