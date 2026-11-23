# Frontend

After you change this layer, update this file so the next run stays current.

Source: `src/powerband/ui/` (Java Swing)

## Implemented
- Ventana del editor (ft-8): botones, lista de grabaciones, monitor en vivo y barra de estado: `src/powerband/ui/EditorWindow.java`
- Monitor de comandos enviados a SuperCollider: `src/powerband/ui/MonitorPanel.java`
- Lista cronológica leída de disco fila por fila: `src/powerband/ui/RecordingListModel.java`
- Toda la lógica y las reglas de estado están en `src/powerband/editor/EditorController.java` (mapa de backend).

## Entrypoints
- `src/powerband/PowerBandMain.java` crea `EditorWindow` con `SwingUtilities.invokeLater`.

## Classes
### EditorWindow
`JFrame` + `IEditorView`. Botones Grabar, Detener grabación, Cargar, Reproducir, Detener reproducción; se activan en `applyState()` según `EditorState`, la selección y la grabación cargada. Cada acción atrapa `PowerBandException`/`IllegalStateException` y muestra un diálogo. Cierre con `DO_NOTHING_ON_CLOSE` + `windowClosing` → `shutdown`, `dispose` y baja el latch. `src/powerband/ui/EditorWindow.java`
- `EditorWindow(EditorController pController, CountDownLatch pClosed, ISynthCommand... pCommands)` — registra el monitor en los comandos
- `stateChanged(EditorState pState)`, `recordingsChanged()`, `showError(String pMessage)` — pasan al hilo de Swing con `invokeLater`

### MonitorPanel
`JScrollPane` con `JTextArea` monoespaciado; `ISynthEventListener`. Máximo `MONITOR_MAX_LINES` líneas. `src/powerband/ui/MonitorPanel.java`
- `MonitorPanel()`
- `onSynthEvent(SynthEvent pEvent)` — formatea con `EditorController.formatEvent` y agrega con `invokeLater`

### RecordingListModel
`AbstractListModel<String>` que no copia la lista: cada fila sale de `controller.recordingAt(i)`. `src/powerband/ui/RecordingListModel.java`
- `RecordingListModel(EditorController pController)`
- `getSize()`, `getElementAt(int pIndex)` — `LIST_READ_ERROR` si falla la lectura
- `nameAt(int pIndex)` — nombre sin fecha (`throws StorageException`)
- `refresh()` — relee el conteo y avisa a la lista
