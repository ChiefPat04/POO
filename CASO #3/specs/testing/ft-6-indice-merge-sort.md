---
id: ft-6
layer: testing
status: approved 
---

# Índice: Balanced Merge Sort y búsqueda binaria

## General description

- Pruebas JUnit 5 de `RecordingIndex` sobre archivos en `@TempDir`: formato, ordenamiento, búsqueda binaria y limpieza de temporales.
- Una prueba de integración con `Recorder` y `RecordingStorage` reales confirma que el índice apunta al encabezado correcto.
- Nunca tocan `data/`.

## Inputs

- Nombres de prueba escritos en la prueba; para cantidades grandes, generados con `Random` de semilla fija.
- `@TempDir` para el índice, los temporales y los archivos de ft-5.

## Outputs

- Resultados de `mvn test`.

## Test criteria

- `RecordingIndexTest`
  - `emptyIndexHasNoEntries`: archivo inexistente → `count()` = 0 y `find` vacío.
  - `addOneEntryCanBeFound`.
  - `recordsHaveFixedSize`: después de varios `add`, largo del archivo = `count() × INDEX_RECORD_SIZE`.
  - `entriesAreSortedAfterEachAdd`: con nombres desordenados, después de **cada** `add` los registros leídos con `readEntry(0..n−1)` están ordenados y son exactamente los agregados.
  - `sortsReverseOrderInput` y `sortsAlreadySortedInput`.
  - `sortsCountsThatAreNotPowersOfTwo`: 3, 5, 7 y 13 registros.
  - `orderIsCaseSensitive`: `"cancion1"`, `"Cancion1"`, `"CANCION1"` → `CANCION1`, `Cancion1`, `cancion1`; los tres se encuentran con su propia posición.
  - `findsFirstMiddleAndLast`.
  - `findsEveryEntryOfManyRecords`: 100 nombres aleatorios (semilla fija); cada uno se encuentra con su posición.
  - `missingNameIsEmpty`: nombre menor que todos, mayor que todos y uno intermedio que no está.
  - `nameWithMaxLengthIsKept`: un nombre de exactamente `NAME_LENGTH` caracteres se guarda y se encuentra.
  - `noTempFilesRemainAfterAdd`: en la carpeta solo queda `recordings.index`.
  - `failedSortLeavesIndexUnchanged`: con la carpeta de temporales sin permiso de escritura o inexistente y no creable (por ejemplo, una ruta que es un archivo), `add` lanza `StorageException` y el índice sigue igual que antes.
- `RecordingIndexRecorderTest` (integración, en `src/test/powerband/storage/`):
  - `savedRecordingIsFoundInIndex`: con `Recorder` + `RecordingStorage` + `RecordingIndex` reales, grabar y guardar 3 grabaciones; `find(nombre)` da una posición donde `readHeader` lee ese nombre.

## Assets

- Para el `Recorder` se copia el armado de `RecorderTest`: `OscReceiver` local (`src/test/powerband/command/helpers/`) + `OscClient` a su puerto + `PianoCommand`, `BassCommand` y `OrganCommand` reales. Así no hace falta `scsynth`.
- Nombres de prueba: solo ASCII imprimible, sin espacios al inicio ni al final (regla de `RecordingStorage.isValidName`).

## Implementation guidelines

- JUnit 5 (`org.junit.jupiter`), ya en `pom.xml`; archivos `src/test/powerband/storage/RecordingIndexTest.java` y `RecordingIndexRecorderTest.java`.
- Clases `<ClassName>Test`; métodos que describen comportamiento.
- En las pruebas sí se pueden usar listas para armar los datos esperados y comparar; la regla de "todo en disco" es para el código de producción.
- Tamaños y nombres de archivo desde `IConstants`.
- Para `failedSortLeavesIndexUnchanged`, leer los bytes del índice antes y después y compararlos.

## Business workflow

1. Crear `RecordingIndex` con un índice y una carpeta de temporales dentro de `@TempDir`.
2. Agregar nombres con posiciones conocidas.
3. Verificar tamaño, orden (con `readEntry`), búsqueda (`find`) y que no quedan temporales.
4. Integración: grabar con `Recorder`, guardar y buscar en el índice.
