package powerband.instrument.helpers;

import java.util.concurrent.atomic.AtomicInteger;

import powerband.exception.DeviceException;
import powerband.instrument.IGamepadReader;
import powerband.model.GamepadState;

/**
 * Control falso: devuelve el estado que la prueba le ponga y cuenta las lecturas.
 */
public class FakeGamepadReader implements IGamepadReader {

    private volatile GamepadState state = GamepadState.idle();
    private volatile boolean failOnOpen;
    private volatile boolean closed;
    private final AtomicInteger readCount = new AtomicInteger();

    @Override
    public void open() throws DeviceException {
        if (failOnOpen) {
            throw new DeviceException("control falso sin conectar");
        }
    }

    @Override
    public GamepadState read() {
        readCount.incrementAndGet();
        return state;
    }

    @Override
    public void close() {
        closed = true;
    }

    public void setState(GamepadState pState) {
        state = pState;
    }

    public void setFailOnOpen(boolean pFail) {
        failOnOpen = pFail;
    }

    public int getReadCount() {
        return readCount.get();
    }

    public boolean isClosed() {
        return closed;
    }
}
