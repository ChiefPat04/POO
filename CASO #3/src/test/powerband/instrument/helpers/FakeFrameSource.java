package powerband.instrument.helpers;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import powerband.exception.DeviceException;
import powerband.instrument.IFrameSource;

/**
 * Webcam falsa: entrega en ciclo los cuadros que la prueba le ponga.
 */
public class FakeFrameSource implements IFrameSource {

    private volatile BufferedImage[] frames = {TestFrames.blank()};
    private volatile boolean hasFrames = true;
    private volatile boolean failOnOpen;
    private volatile boolean closed;
    private final AtomicInteger reads = new AtomicInteger();
    private final AtomicInteger emptyReads = new AtomicInteger();

    @Override
    public void open() throws DeviceException {
        if (failOnOpen) {
            throw new DeviceException("webcam falsa sin conectar");
        }
    }

    @Override
    public BufferedImage read() {
        try {
            Thread.sleep(1); // una cámara real tarda en dar el cuadro; así el hilo no gira en vacío
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        if (!hasFrames) {
            emptyReads.incrementAndGet();
            return null;
        }
        BufferedImage[] current = frames;
        return current[Math.floorMod(reads.getAndIncrement(), current.length)];
    }

    @Override
    public void close() {
        closed = true;
    }

    public void setFrames(BufferedImage... pFrames) {
        frames = pFrames.clone();
    }

    public void setHasFrames(boolean pHasFrames) {
        hasFrames = pHasFrames;
    }

    public void setFailOnOpen(boolean pFail) {
        failOnOpen = pFail;
    }

    public boolean isClosed() {
        return closed;
    }

    /** Lecturas que devolvieron {@code null}. */
    public int getEmptyReads() {
        return emptyReads.get();
    }
}
