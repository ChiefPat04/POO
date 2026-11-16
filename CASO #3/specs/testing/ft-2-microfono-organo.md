---
id: ft-2
layer: testing
status: draft
---

# Micrófono → órgano

## General description

- Pruebas JUnit 5 de `PitchDetector` y de las reglas de `MicrophoneInstrument` con señales sintéticas: senos, silencio y ruido.
- Usan una fuente de audio falsa y un comando que guarda eventos. Sin micrófono y sin SuperCollider.
- `MicrophoneSource` no tiene prueba automática (necesita el micrófono); se prueba a mano con `PowerBandMain`.

## Inputs

- Bloques de `AUDIO_BLOCK_SIZE` muestras generados por `TestSignals`.
- `FakeAudioSource` como `IAudioSource`.
- `RecordingCommand` (ya existe) como `ISynthCommand`.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `PitchDetectorTest`
  - `detectsA440`: frecuencia entre 435.6 y 444.4; `toMidiNote` = 69.
  - `detectsEverySemitoneFromFortyToOneHundred`: seno en la frecuencia exacta de cada nota → la misma nota.
  - `detectsFundamentalOfSineWithHarmonics`: 220 Hz + 2.º armónico a 0.5 + 3.º a 0.25 → 57.
  - `detectionDoesNotDependOnVolume`: seno de 440 Hz con amplitud 0.01 → 69.
  - `returnsNoPitchForSilence`.
  - `returnsNoPitchForWhiteNoise`: `Random` con semilla fija.
  - `rmsOfSilenceIsZero`.
  - `rmsOfSineIsAmplitudeOverSquareRootOfTwo`: amplitud 0.5 → 0.3536 ± 0.01.
  - `toMidiNoteRoundsToNearestSemitone`: 440 → 69, 452 → 69, 455 → 70, 261.63 → 60.
- `MicrophoneInstrumentTest` — reglas, llamando `process` directo:
  - `stableLoudNoteSendsOrganOn`: `MIC_STABLE_WINDOWS` ventanas de 440 Hz → un `NOTE_ON` de la nota 69.
  - `noteDoesNotPlayBeforeStableWindows`: una ventana menos → nada.
  - `heldNoteIsNotRepeated`: 10 ventanas más → sigue siendo un solo evento.
  - `ampFollowsVolumeAndIsClamped`: un seno suave da `MIC_MIN_AMP`, uno fuerte `MIC_MAX_AMP`, uno intermedio RMS × `MIC_AMP_GAIN`.
  - `noteChangeSendsOffThenOn`: 440 Hz y luego 523.25 Hz → `ON 69`, `OFF 69`, `ON 72`.
  - `briefDifferentNoteDoesNotChangeNote`: una sola ventana de 523.25 Hz entre ventanas de 440 Hz → solo `ON 69`.
  - `sustainedSilenceSendsOrganOff`: `MIC_OFF_WINDOWS` ventanas de silencio → `OFF 69`.
  - `shortSilenceKeepsNoteOn`: una ventana menos de silencio y vuelve el tono → sin `OFF`.
  - `volumeBetweenThresholdsKeepsNoteOn`: muchas ventanas con RMS entre los dos umbrales → sin `OFF`.
  - `toneBetweenThresholdsDoesNotStartNote`: sin nota encendida, ese mismo volumen no enciende nada.
  - `silenceNeverPlays`, `noiseNeverPlays`.
  - `outOfRangeNotesAreNotPlayed`: notas 39 (77.78 Hz) y 101 (2793.83 Hz).
  - `neverMoreThanOneNoteOn`: secuencia larga con cambios de nota y silencios; recorriendo los eventos, las notas encendidas nunca pasan de 1 y cada `OFF` es de la nota encendida.
- `MicrophoneInstrumentTest` — ciclo de vida, con el hilo real:
  - `startThrowsDeviceExceptionWhenSourceCannotOpen`.
  - `startTwiceThrowsIllegalStateException`.
  - `threadPlaysNoteFromSource`: la fuente falsa entrega 440 Hz y llega `ON 69` antes de un timeout.
  - `stopReleasesActiveNoteAndClosesSource`: el último evento es `OFF 69` y la fuente quedó cerrada.
  - `sourceWithoutAudioReleasesNoteAndThreadKeepsRunning`: la fuente pasa a devolver `false` → `OFF 69`; al volver el audio, otro `ON 69`.
  - `stopWithoutStartDoesNotFail`.
- Ninguna prueba depende del micrófono ni de `scsynth`.

## Assets

- `src/test/powerband/instrument/helpers/TestSignals.java`: métodos estáticos `sine(float pFrequency, float pAmplitude)`, `harmonics(float pFrequency, float pAmplitude)`, `silence()`, `noise(long pSeed, float pAmplitude)`; cada uno devuelve un bloque de `AUDIO_BLOCK_SIZE` a `AUDIO_SAMPLE_RATE`. También `noteFrequency(int pNote)`.
- `src/test/powerband/instrument/helpers/FakeAudioSource.java`: `IAudioSource` con `setBlock(float[] pBlock)` (campo `volatile`; `read` lo copia y duerme 1 ms para no girar en vacío), `setHasAudio(boolean pHasAudio)`, `setFailOnOpen(boolean pFail)`, `isClosed()`.
- Reutiliza `src/test/powerband/instrument/helpers/RecordingCommand.java`.

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; fuentes en `src/test/`.
- Archivos: `src/test/powerband/instrument/PitchDetectorTest.java` y `MicrophoneInstrumentTest.java`.
- Clases `<ClassName>Test`; métodos que describen comportamiento.
- Umbrales, cantidades de ventanas, rangos y tamaño de bloque desde `IConstants`; las frecuencias y notas esperadas sí van escritas en la prueba.
- Las amplitudes de prueba se calculan a partir de los umbrales (por ejemplo, un RMS a medio camino entre `MIC_OFF_THRESHOLD` y `MIC_ON_THRESHOLD`), para que sigan valiendo si se ajustan los umbrales.
- Las pruebas con hilo esperan con un ciclo corto y un timeout máximo, no con un `sleep` fijo; siempre llaman `stop()` en `finally`.
- Floats con tolerancia.

## Business workflow

1. Detector: generar la señal con `TestSignals`, llamar `detectFrequency` / `rms` / `toMidiNote` y comparar.
2. Reglas: crear `RecordingCommand`, `FakeAudioSource` y `MicrophoneInstrument`; llamar `process` con una secuencia de bloques y comparar los eventos guardados.
3. Ciclo de vida: `start()`, cambiar el bloque de la fuente falsa, esperar el evento, `stop()`.
4. Verificar acción, nota, `amp`, cantidad de eventos y estado de la fuente.
