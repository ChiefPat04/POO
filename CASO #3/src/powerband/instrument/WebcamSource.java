package powerband.instrument;

import static powerband.util.IConstants.ERR_NO_WEBCAM;
import static powerband.util.IConstants.ERR_WEBCAM_OPEN;

import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamResolution;

import powerband.exception.DeviceException;

/**
 * Webcam por defecto del sistema, con la librería Webcam Capture, a 320×240.
 *
 * <p>En Windows, si el permiso de cámara está apagado o otra aplicación la está
 * usando, no aparece o no abre: ambos casos terminan en {@link DeviceException}.</p>
 */
public class WebcamSource implements IFrameSource {

    private volatile Webcam webcam;

    @Override
    public void open() throws DeviceException {
        try {
            Webcam found = Webcam.getDefault();
            if (found == null) {
                throw new DeviceException(ERR_NO_WEBCAM);
            }
            found.setViewSize(WebcamResolution.QVGA.getSize());
            if (!found.open()) {
                throw new DeviceException(ERR_WEBCAM_OPEN);
            }
            webcam = found;
        } catch (WebcamException | IllegalStateException | UnsatisfiedLinkError e) {
            throw new DeviceException(ERR_WEBCAM_OPEN, e);
        }
    }

    @Override
    public BufferedImage read() {
        Webcam current = webcam;
        if (current == null || !current.isOpen()) {
            return null;
        }
        try {
            return current.getImage();
        } catch (WebcamException e) {
            return null; // la cámara se desconectó; el instrumento reintenta
        }
    }

    @Override
    public void close() {
        Webcam current = webcam;
        if (current != null) {
            webcam = null;
            current.close();
        }
    }
}
