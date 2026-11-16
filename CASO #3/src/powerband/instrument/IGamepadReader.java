package powerband.instrument;

import powerband.exception.DeviceException;
import powerband.model.GamepadState;

/**
 * Lectura del control físico. Separa a {@link GamepadInstrument} de la librería
 * que habla con el dispositivo, para poder probar el instrumento sin control.
 */
public interface IGamepadReader {

    /** Inicializa la lectura. Falla si no hay control conectado. */
    void open() throws DeviceException;

    /** Estado actual. Nunca lanza: sin control devuelve {@link GamepadState#disconnected()}. */
    GamepadState read();

    /** Libera el dispositivo. */
    void close();
}
