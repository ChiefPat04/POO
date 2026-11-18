package powerband.model;

import static powerband.util.IConstants.CAM_NO_MOTION;
import static powerband.util.IConstants.CAM_NORMALIZED_MIN;

/**
 * Lo que la webcam vio de la mano en un cuadro. Inmutable.
 *
 * <p>{@code x} y {@code y} van de 0 a 1 (0 = izquierda / arriba de la imagen que
 * ve el usuario). {@code motion} es la fracción de la imagen que cambió desde el
 * cuadro anterior.</p>
 */
public final class HandReading {

    private final boolean present;
    private final float x;
    private final float y;
    private final float motion;

    public HandReading(boolean pPresent, float pX, float pY, float pMotion) {
        present = pPresent;
        x = pX;
        y = pY;
        motion = pMotion;
    }

    /** No se vio ninguna mano moviéndose. */
    public static HandReading absent() {
        return new HandReading(false, CAM_NORMALIZED_MIN, CAM_NORMALIZED_MIN, CAM_NO_MOTION);
    }

    public boolean isPresent() {
        return present;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getMotion() {
        return motion;
    }
}
