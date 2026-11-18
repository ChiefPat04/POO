package powerband.exception;

import static powerband.util.IConstants.ERR_RECORDING_NOT_FOUND;

/**
 * El nombre pedido no está en recordings.index. No extiende {@link StorageException}:
 * no es una falla de disco y el editor la atiende avisándole al usuario.
 */
public class RecordingNotFoundException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public RecordingNotFoundException(String pName) {
        super(ERR_RECORDING_NOT_FOUND + pName);
    }
}
