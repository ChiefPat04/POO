package powerband.instrument;

import java.awt.image.BufferedImage;

import powerband.exception.DeviceException;

/**
 * Fuente de cuadros de video. Separa a {@link WebcamInstrument} de la cámara
 * real para poder probarlo con imágenes generadas.
 */
public interface IFrameSource {

    /** Abre la cámara. Falla si no hay ninguna o no se puede abrir. */
    void open() throws DeviceException;

    /**
     * Cuadro actual.
     *
     * @return {@code null} si no hay cuadro. Nunca lanza.
     */
    BufferedImage read();

    /** Cierra la cámara. */
    void close();
}
