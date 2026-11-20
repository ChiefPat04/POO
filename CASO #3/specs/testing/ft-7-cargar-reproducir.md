---
id: ft-7
layer: testing
status: approved
---

# Cargar y reproducir

## General description

- Pruebas JUnit 5 de `RecordingLibrary` y `Player` sobre archivos en `@TempDir`, escritos con `RecordingStorage` y `RecordingIndex` reales.
- Los comandos son falsos y guardan cada evento con la hora en que llegó. Sin SuperCollider.
- Nunca tocan `data/`.

## Inputs

- Grabaciones armadas en la prueba: encabezado + comandos con `timeMs` conocidos, escritos con `RecordingStorage.appendHeader` / `appendEvent` / `writeHeader`, más `appendListEntry` e `RecordingIndex.add`.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `RecordingLibraryTest`
  - `listsRecordingsInChronologicalOrder`.
  - `loadReturnsHeaderOfSavedRecording`.
  - `loadUnknownNameThrows`.
  - `loadIsCaseSensitive`.
- `PlayerTest`
  - `sendsEachEventToItsSynthInOrder`: grabación con piano, bajo, órgano on y órgano off → cada uno llega a su comando falso, en orden y con los mismos parámetros.
  - `playsOnlyTheChosenRecording`: con 2 grabaciones en el archivo, reproducir la segunda no envía nada de la primera.
  - `keepsRecordedIntervals`: comandos en 0, 100 y 250 ms → las distancias entre llegadas son 100 y 150 ms ± `PLAYER_TIMING_TOLERANCE_MS`.
  - `timingDoesNotDependOnLoadTime`: cargar, esperar 200 ms y reproducir → el primer comando sale de inmediato y las distancias se mantienen.
  - `emptyRecordingSendsNothing`.
  - `startRunsInBackgroundAndNotifiesWhenDone`: `start` vuelve antes de que llegue el último comando; al final `isPlaying()` es `false` y el listener se llamó una vez con el nombre.
  - `stopTurnsOffHeldOrganNotes`: grabación con `organOn` en 0 ms y `organOff` en 2000 ms; `stop` a los 100 ms → llega un `NOTE_OFF` de esa nota, no llega nada más de la grabación y el listener se llamó.
  - `startWhilePlayingThrows`.
  - `stopWithoutPlayingDoesNotFail`.
  - `sendFailureDoesNotStopPlayback`: el comando del bajo lanza `SynthConnectionException`; los comandos de piano siguientes igual llegan.
- Ninguna prueba depende de `scsynth`.

## Assets

- `src/test/powerband/editor/helpers/TimedCommand.java`: `ISynthCommand` falso que guarda cada evento con `System.currentTimeMillis()` al recibirlo; `getEvents()`, `getTimes()`, `setFailing(boolean pFail)` (lanza `SynthConnectionException`).
- `src/test/powerband/editor/helpers/RecordingFiles.java`: arma storage + índice en una carpeta y tiene `save(String pName, SynthEvent... pEvents)` que escribe encabezado, comandos (con su `timeMs`), lista e índice.

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; archivos `src/test/powerband/editor/RecordingLibraryTest.java` y `PlayerTest.java`.
- Tolerancia de tiempo y nombres de archivo desde `IConstants`.
- Las pruebas con hilo esperan con un ciclo corto y un timeout máximo, no con un `sleep` fijo; siempre llaman `stop()` en `finally`.
- Los tiempos de las grabaciones de prueba son cortos (menos de 1 s, salvo `stopTurnsOffHeldOrganNotes`) para que la suite siga rápida.

## Business workflow

1. Escribir grabaciones de prueba con `RecordingFiles.save`.
2. Biblioteca: listar, cargar por nombre y verificar el encabezado o la excepción.
3. Reproductor: `play` o `start` con el encabezado cargado y verificar eventos, comandos destino, orden, tiempos, notas apagadas y aviso al listener.
