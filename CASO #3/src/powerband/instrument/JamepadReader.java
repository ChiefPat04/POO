package powerband.instrument;

import static powerband.util.IConstants.ERR_GAMEPAD_INIT;
import static powerband.util.IConstants.ERR_NO_GAMEPAD;
import static powerband.util.IConstants.GAMEPAD_INDEX;
import static powerband.util.IConstants.NO_GAMEPADS;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import powerband.exception.DeviceException;
import powerband.model.GamepadState;

/**
 * Lee el control (DualSense por USB o Bluetooth) con la librería Jamepad (SDL).
 * En SDL los botones se llaman a/b/x/y: equivalen a equis/círculo/cuadrado/triángulo.
 */
public class JamepadReader implements IGamepadReader {

    private volatile ControllerManager manager;

    @Override
    public void open() throws DeviceException {
        ControllerManager opened;
        try {
            opened = new ControllerManager();
            opened.initSDLGamepad();
        } catch (RuntimeException | UnsatisfiedLinkError e) {
            throw new DeviceException(ERR_GAMEPAD_INIT, e);
        }
        opened.update();
        if (opened.getNumControllers() == NO_GAMEPADS) {
            opened.quitSDLGamepad();
            throw new DeviceException(ERR_NO_GAMEPAD);
        }
        manager = opened;
    }

    @Override
    public GamepadState read() {
        ControllerManager current = manager;
        if (current == null) {
            return GamepadState.disconnected();
        }
        try {
            ControllerState state = current.getState(GAMEPAD_INDEX);
            if (!state.isConnected) {
                return GamepadState.disconnected();
            }
            return new GamepadState(state.a, state.b, state.x, state.y, state.lb, state.rb,
                    state.leftStickX, state.rightTrigger, true);
        } catch (IllegalStateException e) {
            // Jamepad ya no está inicializado: para el instrumento es "sin control"
            return GamepadState.disconnected();
        }
    }

    @Override
    public void close() {
        ControllerManager current = manager;
        if (current != null) {
            manager = null;
            current.quitSDLGamepad();
        }
    }
}
