package powerband.model;

import static powerband.util.IConstants.GAMEPAD_AXIS_REST;

/**
 * Foto del estado del control en un instante. Inmutable.
 */
public final class GamepadState {

    private final boolean cross;
    private final boolean circle;
    private final boolean square;
    private final boolean triangle;
    private final boolean l1;
    private final boolean r1;
    private final float leftStickX;
    private final float rightTrigger;
    private final boolean connected;

    public GamepadState(boolean pCross, boolean pCircle, boolean pSquare, boolean pTriangle,
            boolean pL1, boolean pR1, float pLeftStickX, float pRightTrigger,
            boolean pConnected) {
        cross = pCross;
        circle = pCircle;
        square = pSquare;
        triangle = pTriangle;
        l1 = pL1;
        r1 = pR1;
        leftStickX = pLeftStickX;
        rightTrigger = pRightTrigger;
        connected = pConnected;
    }

    /** Estado cuando no hay control. */
    public static GamepadState disconnected() {
        return new GamepadState(false, false, false, false, false, false,
                GAMEPAD_AXIS_REST, GAMEPAD_AXIS_REST, false);
    }

    /** Control conectado, sin nada presionado. */
    public static GamepadState idle() {
        return new GamepadState(false, false, false, false, false, false,
                GAMEPAD_AXIS_REST, GAMEPAD_AXIS_REST, true);
    }

    public boolean isCross() {
        return cross;
    }

    public boolean isCircle() {
        return circle;
    }

    public boolean isSquare() {
        return square;
    }

    public boolean isTriangle() {
        return triangle;
    }

    public boolean isL1() {
        return l1;
    }

    public boolean isR1() {
        return r1;
    }

    /** Stick izquierdo horizontal: -1 (izquierda) a 1 (derecha). */
    public float getLeftStickX() {
        return leftStickX;
    }

    /** Gatillo R2: 0 (suelto) a 1 (a fondo). */
    public float getRightTrigger() {
        return rightTrigger;
    }

    public boolean isConnected() {
        return connected;
    }
}
