package powerband.exception;

/**
 * Excepción base (checked) de Power Band. Todas las excepciones propias la extienden.
 */
public class PowerBandException extends Exception {

    private static final long serialVersionUID = 1L;

    public PowerBandException(String pMessage) {
        super(pMessage);
    }

    public PowerBandException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
