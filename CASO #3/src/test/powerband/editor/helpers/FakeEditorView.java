package powerband.editor.helpers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import powerband.editor.EditorState;
import powerband.editor.IEditorView;

/**
 * Vista falsa: guarda los avisos del controlador. Segura para hilos, porque el fin
 * de una reproducción llega desde el hilo del reproductor.
 */
public class FakeEditorView implements IEditorView {

    private final List<EditorState> states = new CopyOnWriteArrayList<>();
    private final List<String> errors = new CopyOnWriteArrayList<>();
    private final AtomicInteger recordingsChanged = new AtomicInteger();

    @Override
    public void stateChanged(EditorState pState) {
        states.add(pState);
    }

    @Override
    public void recordingsChanged() {
        recordingsChanged.incrementAndGet();
    }

    @Override
    public void showError(String pMessage) {
        errors.add(pMessage);
    }

    public List<EditorState> getStates() {
        return states;
    }

    public List<String> getErrors() {
        return errors;
    }

    public int getRecordingsChanged() {
        return recordingsChanged.get();
    }
}
