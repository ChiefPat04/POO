---
id: ft-5
layer: testing
status: draft
---

# Grabación de música

## General description

- Pruebas JUnit 5 del formato binario (`RecordingStorage`), del flujo del `Recorder` y de `OrganCommand.allNotesOff`.
- Usan archivos temporales (`@TempDir`); nunca tocan `data/`. Sin SuperCollider ni dispositivos.

## Inputs

- `RecordingStorage` sobre archivos dentro de un `@TempDir`.
- `SynthEvent` creados con las fábricas y `withTimeMs`.
- Comandos reales con un `OscClient` apuntando al `OscReceiver` de prueba (puerto efímero local).
- `FakeIndexer` que guarda las llamadas a `add`.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `RecordingStorageTest`
  - `headerRoundTripKeepsNameAndPositions`.
  - `shortNameIsPaddedWithSpacesToThirtyTwoBytes`: revisa los bytes crudos del archivo.
  - `commandRoundTripKeepsAllFieldsForPianoBassAndOrgan`: un evento de cada synth/acción.
  - `everyRecordMeasuresRecordSize`: tras 1 encabezado + 3 comandos, largo = `4 * RECORD_SIZE`.
  - `firstByteIsRecordType`: 1 en el encabezado, 2 en los comandos.
  - `writeHeaderRewritesInPlaceWithoutChangingLength`: el comando siguiente se lee igual.
  - `readHeaderOnCommandRecordThrowsStorageException` y `readEventOnHeaderRecordThrowsStorageException`.
  - `listEntriesAreReadInAppendOrder`.
  - `listContainsNameIsCaseSensitive`: con `"Cancion1"` guardado, `"cancion1"` → `false`.
  - `listContainsNameReturnsFalseWhenFileDoesNotExist`.
  - `truncateMusicRemovesTrailingRecords`.
  - `isValidNameRejectsNullEmptyTooLongNonAsciiAndEdgeSpaces`: `"canción"`, `"año"`, 33 caracteres, `" a"`, `"a "`.
  - `createsDataDirectoryWhenMissing`.
- `RecorderTest`
  - `ignoresEventsBeforeStartAndAfterStop`.
  - `startWritesProvisionalHeader`: nombre vacío, inicio = posición + `RECORD_SIZE`, fin = `PENDING_POSITION`.
  - `recordsRelativeTime`: evento con `withTimeMs(recorder.getStartTimeMs() + 500)` → 500 en disco.
  - `clampsNegativeRelativeTimeToZero`.
  - `stopRecordsNoteOffForEveryActiveOrganNote`: dos `organOn` por el `OrganCommand` real, luego `stop`; los dos últimos registros son `NOTE_OFF` de esas notas.
  - `saveRewritesHeaderAppendsListEntryAndCallsIndexer`.
  - `saveWithDuplicateNameThrowsAndAllowsRetry`: archivos sin cambios, indexador sin llamadas; un segundo `save` con otro nombre funciona.
  - `saveWithInvalidNameThrowsIllegalArgumentException`.
  - `namesDifferingOnlyInCaseAreBothAccepted`.
  - `discardRestoresMusicFileLength`.
  - `secondRecordingStartsAfterFirstAndLeavesItUntouched`.
  - `concurrentEventsAreAllRecordedAsWholeRecords`: varios hilos llaman `onSynthEvent`; cantidad de comandos = eventos enviados y largo múltiplo de `RECORD_SIZE`.
  - `stopRethrowsStorageFailureFromOnSynthEvent`: se borra el `@TempDir` (o se usa una ruta cuyo padre es un archivo) después de `start`.
  - `wrongStateCallsThrowIllegalStateException`: `stop`/`save`/`discard` en `IDLE`, `start` grabando.
- `OrganCommandTest` (agregar)
  - `allNotesOffReleasesEveryActiveNodeAndNotifies`.
  - `allNotesOffWithNoActiveNotesSendsNothing`.
- Ninguna prueba depende de `scsynth`; todas pasan con `mvn test`.

## Assets

- `src/test/powerband/editor/helpers/FakeIndexer.java`: `IRecordingIndexer` que guarda nombre y posición de cada `add`.
- Reutiliza `src/test/powerband/command/helpers/OscReceiver.java`.

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; fuentes en `src/test/`.
- Paquetes espejo: `src/test/powerband/storage/RecordingStorageTest.java`, `src/test/powerband/editor/RecorderTest.java`, `src/test/powerband/command/OrganCommandTest.java`.
- Clases `<ClassName>Test`; métodos que describen comportamiento.
- Tamaños, tipos y códigos desde `IConstants`, no escritos en la prueba.
- Lo grabado se verifica leyendo con `RecordingStorage.readHeader`/`readEvent`/`readListEntry`; los bytes crudos solo donde el criterio es el formato (relleno, primer byte).
- Clientes, receptores y archivos en try-with-resources.
- Floats con tolerancia pequeña.

## Business workflow

1. Crear `RecordingStorage` con archivos del `@TempDir`.
2. Abrir `OscReceiver`, crear `OscClient` y los 3 comandos; crear `Recorder` con `FakeIndexer`.
3. `start`, enviar eventos (por los comandos o directo a `onSynthEvent`), `stop`.
4. `save` o `discard`.
5. Leer los archivos con `RecordingStorage` y comparar con lo esperado.
6. Revisar las llamadas del `FakeIndexer`.
