package powerband.instrument;

import powerband.exception.DeviceException;

/**
 * Fuente de audio en bloques de muestras normalizadas (-1 a 1). Separa a
 * {@link MicrophoneInstrument} del micrófono real para poder probarlo con
 * señales generadas.
 */
public interface IAudioSource {

    /** Abre la captura. Falla si no hay dispositivo o no se puede abrir. */
    void open() throws DeviceException;

    /**
     * Llena el bloque completo; bloquea hasta tenerlo.
     *
     * @return {@code false} si no hay audio. Nunca lanza.
     */
    boolean read(float[] pBlock);

    /** Cierra la captura. */
    void close();
}
