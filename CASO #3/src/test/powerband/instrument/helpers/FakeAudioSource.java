package powerband.instrument.helpers;

import powerband.exception.DeviceException;
import powerband.instrument.IAudioSource;

/**
 * Micrófono falso: entrega una y otra vez el bloque que la prueba le ponga.
 */
public class FakeAudioSource implements IAudioSource {

    private volatile float[] block = TestSignals.silence();
    private volatile boolean hasAudio = true;
    private volatile boolean failOnOpen;
    private volatile boolean closed;

    @Override
    public void open() throws DeviceException {
        if (failOnOpen) {
            throw new DeviceException("micrófono falso sin conectar");
        }
    }

    @Override
    public boolean read(float[] pBlock) {
        if (!hasAudio) {
            return false;
        }
        float[] current = block;
        System.arraycopy(current, 0, pBlock, 0, pBlock.length);
        try {
            Thread.sleep(1); // un micrófono real bloquea; así el hilo no gira en vacío
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        closed = true;
    }

    public void setBlock(float[] pBlock) {
        block = pBlock;
    }

    public void setHasAudio(boolean pHasAudio) {
        hasAudio = pHasAudio;
    }

    public void setFailOnOpen(boolean pFail) {
        failOnOpen = pFail;
    }

    public boolean isClosed() {
        return closed;
    }
}
