package powerband.ui;

import static powerband.util.IConstants.FIRST_INDEX;
import static powerband.util.IConstants.LIST_READ_ERROR;

import javax.swing.AbstractListModel;

import powerband.editor.EditorController;
import powerband.exception.StorageException;

/**
 * Lista de grabaciones en orden cronológico. No copia la lista a memoria: cada
 * fila se lee de recordings.list cuando Swing la pinta.
 */
public class RecordingListModel extends AbstractListModel<String> {

    private static final long serialVersionUID = 1L;

    private final transient EditorController controller;
    private int size;

    public RecordingListModel(EditorController pController) {
        controller = pController;
        refresh();
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String getElementAt(int pIndex) {
        try {
            return EditorController.formatListEntry(controller.recordingAt(pIndex));
        } catch (StorageException e) {
            return LIST_READ_ERROR;
        }
    }

    /** Nombre de la grabación de esa fila, sin la fecha. */
    public String nameAt(int pIndex) throws StorageException {
        return controller.recordingAt(pIndex).getName();
    }

    /** Relee la cantidad de grabaciones y avisa a la lista. */
    public void refresh() {
        int oldSize = size;
        try {
            size = (int) controller.recordingCount();
        } catch (StorageException e) {
            size = FIRST_INDEX;
        }
        int last = Math.max(oldSize, size) - 1;
        if (last >= FIRST_INDEX) {
            fireContentsChanged(this, FIRST_INDEX, last);
        }
    }
}
