package powerband.exception;

/**
 * Error de un dispositivo físico: no está conectado o no se pudo inicializar.
 */
public class DeviceException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public DeviceException(String pMessage) {
        super(pMessage);
    }

    public DeviceException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
