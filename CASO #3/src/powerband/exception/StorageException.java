package powerband.exception;

/**
 * Error al leer o escribir los archivos de grabaciones.
 */
public class StorageException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public StorageException(String pMessage) {
        super(pMessage);
    }

    public StorageException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
