package powerband.exception;

/**
 * Error local al comunicarse con scsynth: puerto inválido, host que no resuelve,
 * socket que no se puede crear o datagrama que no se puede enviar.
 * UDP no detecta si scsynth está apagado, así que ese caso no lanza esta excepción.
 */
public class SynthConnectionException extends PowerBandException {

    private static final long serialVersionUID = 1L;

    public SynthConnectionException(String pMessage) {
        super(pMessage);
    }

    public SynthConnectionException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }
}
