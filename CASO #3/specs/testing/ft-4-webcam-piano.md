---
id: ft-4
layer: testing
status: approved 
---

# Webcam → piano

## General description

- Pruebas JUnit 5 de `HandTracker` con imágenes generadas y de las reglas de `WebcamInstrument` con lecturas `HandReading` armadas a mano.
- Usan una fuente de cuadros falsa y un comando que guarda eventos. Sin webcam y sin SuperCollider.
- `WebcamSource` no tiene prueba automática (necesita la cámara); se prueba a mano con `PowerBandMain`.

## Inputs

- Imágenes de `TestFrames`: fondo negro con un cuadrado blanco en una posición dada.
- `FakeFrameSource` como `IFrameSource`.
- `RecordingCommand` (ya existe) como `ISynthCommand`.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `HandTrackerTest`
  - `firstFrameIsAbsent`.
  - `identicalFramesAreAbsent`.
  - `tinyChangeIsAbsent`: un solo píxel distinto.
  - `wholeFrameChangeIsAbsent`: negro → blanco.
  - `movingSquareIsPresentAtItsCenter`: cuadrado de (0.2, 0.5) a (0.3, 0.5) → `x` ≈ 0.25 (o 0.75 si `CAM_MIRROR`) y `y` ≈ 0.5, ± 0.05.
  - `higherSquareGivesSmallerY`.
  - `biggerMovementGivesMoreMotion`.
  - `resetForgetsPreviousFrame`.
- `WebcamInstrumentTest` — reglas, llamando `process` directo:
  - `strikeTriggersOneChord`: una lectura con `motion = CAM_STRIKE_MOTION` → un evento `PIANO`, `TRIGGER`.
  - `sustainedMotionDoesNotRepeat`: 10 lecturas fuertes seguidas → un solo evento.
  - `lowMotionRearms`: fuerte, débil (< `CAM_REARM_MOTION`), fuerte → dos eventos.
  - `absentHandRearms`: fuerte, `absent()`, fuerte → dos eventos.
  - `motionBetweenThresholdsDoesNotRearm`: fuerte, intermedia, fuerte → un evento.
  - `motionBetweenThresholdsDoesNotTrigger`: sin golpe previo, solo intermedias → nada.
  - `topOfImageIsHighestNote` (`y = 0` → 72) y `bottomOfImageIsLowestNote` (`y = 1` → 48).
  - `heightToNoteCoversScaleInOrder`: recorriendo `y` de 1 a 0 las notas suben sin saltarse grados: 48, 50, 52, 53, 55, 57, 59, 60, …, 72.
  - `allNotesAreInRangeAndInCMajor`: muchas alturas, incluidas fuera de 0..1.
  - `leftSideIsMajorRightSideIsMinor`.
  - `ampFollowsMotionAndIsClamped`: golpe justo en el umbral, uno intermedio y uno enorme.
  - `durationIsFixed`: `durMs` = `CAM_DUR_MS`.
- `WebcamInstrumentTest` — ciclo de vida, con el hilo real:
  - `startThrowsDeviceExceptionWhenSourceCannotOpen`.
  - `startTwiceThrowsIllegalStateException`.
  - `threadTriggersChordFromMovingSquare`: la fuente falsa alterna dos cuadros con el cuadrado en lugares distintos y llega un `TRIGGER` antes de un timeout.
  - `sourceWithoutFramesKeepsThreadRunning`: la fuente devuelve `null` un rato y luego vuelve; llega un `TRIGGER`.
  - `stopClosesSource`.
  - `stopWithoutStartDoesNotFail`.
- Ninguna prueba depende de la webcam ni de `scsynth`.

## Assets

- `src/test/powerband/instrument/helpers/TestFrames.java`: métodos estáticos `blank()`, `filled()`, `withSquare(float pX, float pY, float pSize)` (centro y lado normalizados) y `withPixel(int pX, int pY)`; todos devuelven `BufferedImage` de `TEST_FRAME_WIDTH × TEST_FRAME_HEIGHT` (320 × 240, constantes privadas del helper de prueba).
- `src/test/powerband/instrument/helpers/FakeFrameSource.java`: `IFrameSource` con `setFrames(BufferedImage... pFrames)` (los entrega en ciclo; campo `volatile`; `read` duerme 1 ms), `setHasFrames(boolean pHasFrames)`, `setFailOnOpen(boolean pFail)`, `isClosed()`.
- Reutiliza `src/test/powerband/instrument/helpers/RecordingCommand.java`.

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; fuentes en `src/test/`.
- Archivos: `src/test/powerband/instrument/HandTrackerTest.java` y `WebcamInstrumentTest.java`.
- Las pruebas no cargan la librería de la webcam: solo usan `IFrameSource`, `HandTracker` y `java.awt.image`.
- Umbrales, rangos y tamaños de cuadrícula desde `IConstants`; las notas esperadas sí van escritas en la prueba.
- Los `motion` de prueba se calculan a partir de los umbrales (por ejemplo, a medio camino entre `CAM_REARM_MOTION` y `CAM_STRIKE_MOTION`), para que sigan valiendo si se ajustan.
- Las pruebas con hilo esperan con un ciclo corto y un timeout máximo, no con un `sleep` fijo; siempre llaman `stop()` en `finally`.
- Floats con tolerancia.

## Business workflow

1. Tracker: generar cuadros con `TestFrames`, pasarlos en orden a `track` y comparar `present`, `x`, `y`, `motion`.
2. Reglas: crear `RecordingCommand`, `FakeFrameSource` y `WebcamInstrument`; llamar `process` con lecturas armadas y comparar los eventos guardados.
3. Ciclo de vida: `start()`, cambiar los cuadros de la fuente falsa, esperar el evento, `stop()`.
4. Verificar synth, acción, nota, tercera, `amp`, `durMs`, cantidad de eventos y estado de la fuente.
