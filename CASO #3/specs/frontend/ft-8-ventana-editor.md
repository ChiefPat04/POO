---
id: ft-8
layer: frontend
status: draft
---

# Ventana del editor e integración

## General description

- Ventana Swing del editor: 5 botones, la lista de grabaciones, el monitor en vivo de comandos y una barra de estado.
- Solo muestra y llama a `EditorController`; no tiene reglas propias.
- Implementa `IEditorView` y pasa cada aviso al hilo de Swing.

## Inputs

- `EditorController` y los 3 `ISynthCommand` (para registrar el monitor), desde `PowerBandMain`.
- Avisos de `IEditorView` desde cualquier hilo.
- `SynthEvent` desde los hilos de los instrumentos y del `Player`.

## Outputs

- Llamadas al controlador por cada botón y al cerrar.
- Diálogos de nombre y de error.
- Baja el `CountDownLatch` de `PowerBandMain` cuando la ventana se cierra.

## Test criteria

- Botones según el estado:

  | Estado | Grabar | Detener grabación | Cargar | Reproducir | Detener reproducción |
  |---|---|---|---|---|---|
  | `IDLE` | sí | no | sí, si hay una grabación seleccionada | sí, si hay una grabación cargada | no |
  | `RECORDING` | no | sí | no | no | no |
  | `NAMING` | no | no | no | no | no |
  | `PLAYING` | no | no | no | no | sí |

- La lista muestra las grabaciones en orden cronológico (la más vieja arriba), con nombre y fecha/hora; se refresca al guardar.
- El monitor agrega una línea por comando enviado, incluidos los de una reproducción, y nunca pasa de `MONITOR_MAX_LINES`: se borran las más viejas.
- La barra de estado muestra los instrumentos iniciados y fallidos, y el estado actual.
- Detener grabación pide el nombre: nombre inválido o repetido → mensaje de error y se vuelve a pedir; cancelar → se descarta.
- Ningún error de una acción cierra la ventana: se muestra en un diálogo.
- Cerrar la ventana llama `shutdown` una sola vez y baja el latch.
- No hay prueba automática de la ventana (necesita pantalla); se prueba a mano.

## Assets

- `EditorWindow extends JFrame implements IEditorView` (`src/powerband/ui/EditorWindow.java`):
  - `EditorWindow(EditorController pController, CountDownLatch pClosed)`
  - `void stateChanged(EditorState pState)`, `void recordingsChanged()`, `void showError(String pMessage)`
- `MonitorPanel extends JScrollPane implements ISynthEventListener` (`src/powerband/ui/MonitorPanel.java`):
  - `MonitorPanel()`; `void onSynthEvent(SynthEvent pEvent)`
- `RecordingListModel extends AbstractListModel<String>` (`src/powerband/ui/RecordingListModel.java`):
  - `RecordingListModel(EditorController pController)`; `int getSize()`, `String getElementAt(int pIndex)`, `void refresh()`, `String nameAt(int pIndex)`
- Distribución (`BorderLayout`):

  ```
  +--------------------------------------------------------------+
  | [Grabar] [Detener grab.] [Cargar] [Reproducir] [Detener rep.] |  NORTH
  +----------------------+---------------------------------------+
  | Grabaciones          | Monitor                               |
  | Cancion1  2026-11-.. | 14:02:11.512  PIANO  TRIGGER  nota 60 |  CENTER (JSplitPane)
  | Jam       2026-11-.. | 14:02:11.890  ORGAN  NOTE_ON  nota 69 |
  +----------------------+---------------------------------------+
  | Iniciados: Control PS5 → bajo | Fallaron: Webcam (...) | Grabando | SOUTH
  +--------------------------------------------------------------+
  ```

- Claves nuevas en `IConstants`:
  - Textos: `WINDOW_TITLE` (= `APP_NAME`), `BTN_RECORD` (`Grabar`), `BTN_LOAD` (`Cargar`), `STATUS_LOADED` (`Cargada: `), `BTN_STOP_RECORDING` (`Detener grabación`), `BTN_PLAY` (`Reproducir`), `BTN_STOP_PLAYBACK` (`Detener reproducción`), `TITLE_RECORDINGS` (`Grabaciones`), `TITLE_MONITOR` (`Monitor`), `DIALOG_NAME_TITLE` (`Guardar grabación`), `DIALOG_NAME_PROMPT` (`Nombre (máximo 32 caracteres, sin tildes ni ñ):`), `DIALOG_ERROR_TITLE` (`Error`), `STATE_IDLE` (`Listo`), `STATE_RECORDING` (`Grabando…`), `STATE_NAMING` (`Esperando nombre`), `STATE_PLAYING` (`Reproduciendo…`), `LIST_READ_ERROR` (`(no se pudo leer)`), `NEWLINE` (`\n`).
  - Tamaños y límites: `WINDOW_WIDTH` (1000), `WINDOW_HEIGHT` (600), `LIST_PANEL_WIDTH` (360), `MONITOR_MAX_LINES` (500), `MONITOR_FONT_SIZE` (12), `UI_GAP` (8), `MONO_FONT` (`Font.MONOSPACED`).

## Implementation guidelines

- Java Swing, solo stdlib. Archivos en `src/powerband/ui/`: `EditorWindow.java`, `MonitorPanel.java`, `RecordingListModel.java`, más las constantes en `src/powerband/util/IConstants.java`.
- Todo lo que toca componentes corre en el hilo de Swing. Los métodos de `IEditorView` y `onSynthEvent` pueden llegar desde otros hilos, así que envuelven su trabajo en `SwingUtilities.invokeLater`.
- Los botones solo llaman al controlador; habilitarlos o no se decide en un solo método `applyState(EditorState)` con la tabla de Test criteria, más la selección de la lista para Reproducir. Leer `controller.getState()` al construir la ventana.
- Cada acción atrapa `PowerBandException` e `IllegalStateException` y muestra `JOptionPane.showMessageDialog(..., DIALOG_ERROR_TITLE, ERROR_MESSAGE)`. Nunca `catch` vacío.
- Detener grabación:
  1. `controller.stopRecording()`; si lanza, muestra el error y sigue (el estado ya es `NAMING`).
  2. Ciclo con `JOptionPane.showInputDialog`: `null` (cancelar o cerrar) → `controller.discardRecording()` y sale.
  3. Si `!EditorController.isValidName(nombre)` → `ERR_INVALID_NAME` y repite.
  4. `controller.saveRecording(nombre)`; `DuplicateRecordingNameException` → su mensaje y repite; `StorageException` → su mensaje y repite solo si el estado sigue en `NAMING`.
- Cargar: toma `listModel.nameAt(listaSeleccionada)` y llama `controller.load(nombre)`; la barra de estado muestra `STATUS_LOADED` + el nombre. Reproducir llama `controller.play()`.
- `RecordingListModel` no copia la lista: `getSize` devuelve el conteo leído en el último `refresh()`, y `getElementAt` llama `controller.recordingAt(i)` y `formatListEntry`. Si lee mal, devuelve `LIST_READ_ERROR`. `refresh()` relee el conteo y llama `fireContentsChanged`. Así se respeta la regla de no cargar las grabaciones a una colección.
- `nameAt` lee el nombre de la entrada (`ListEntry.getName()`), no el texto formateado.
- `MonitorPanel`: `JTextArea` no editable con fuente monoespaciada. `onSynthEvent` formatea con `EditorController.formatEvent` en el hilo que llama y agrega la línea con `invokeLater`; si pasa de `MONITOR_MAX_LINES`, borra desde el inicio hasta `getLineEndOffset` de la línea sobrante. Baja el scroll al final.
- `EditorWindow` registra el `MonitorPanel` con `addListener` en los 3 comandos. Al cerrar, lo quita con `removeListener`.
- Cierre: `setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)` y un `WindowAdapter.windowClosing` que llama `controller.shutdown()`, `dispose()` y `latch.countDown()`. Una bandera evita hacerlo dos veces.
- La barra de estado es un `JLabel`: `controller.getInstrumentStatus()` + `STATUS_PART_SEPARATOR` + el texto del estado.
- Tamaño inicial `WINDOW_WIDTH × WINDOW_HEIGHT`, centrada con `setLocationRelativeTo(null)`.
- Parámetros con prefijo `p`; campos `private`; ningún literal fuera de `IConstants`.

## Business workflow

1. `PowerBandMain` crea la ventana en el hilo de Swing; la ventana se registra en los comandos y como vista del controlador.
2. El usuario graba, detiene, nombra o cancela; la ventana llama al controlador y refresca botones, lista y estado con cada aviso.
3. El usuario selecciona una grabación y reproduce; el monitor muestra los comandos y, al terminar, los botones vuelven a `IDLE`.
4. Al cerrar, `shutdown`, `dispose` y baja el latch.
