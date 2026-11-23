package powerband.editor;

/**
 * Lo que el {@link EditorController} le avisa a la vista. Se puede llamar desde
 * cualquier hilo (el del reproductor, por ejemplo): la implementación Swing pasa
 * el trabajo al hilo de Swing.
 */
public interface IEditorView {

    void stateChanged(EditorState pState);

    /** Se guardó una grabación: hay que releer la lista. */
    void recordingsChanged();

    void showError(String pMessage);
}
