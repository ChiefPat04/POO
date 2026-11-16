package powerband.exception;

import static powerband.util.IConstants.ERR_DUPLICATE_NAME;

/**
 * El nombre elegido para una grabación ya existe en recordings.list.
 * No extiende {@link StorageException}: no es una falla de disco y el editor
 * la atiende pidiendo otro nombre.
 */
public class DuplicateRecordingNameException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public DuplicateRecordingNameException(String pName) {
        super(ERR_DUPLICATE_NAME + pName);
    }
}
