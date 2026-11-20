package powerband.instrument.helpers;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Cuadros de video generados para probar sin webcam: fondo negro y, si se pide,
 * un cuadrado blanco.
 */
public final class TestFrames {

    private static final int TEST_FRAME_WIDTH = 320;
    private static final int TEST_FRAME_HEIGHT = 240;

    private TestFrames() {
    }

    /** Todo negro. */
    public static BufferedImage blank() {
        return paint(Color.BLACK);
    }

    /** Todo blanco. */
    public static BufferedImage filled() {
        return paint(Color.WHITE);
    }

    /**
     * Fondo negro con un cuadrado blanco.
     *
     * @param pX    centro horizontal, 0..1
     * @param pY    centro vertical, 0..1 (0 = arriba)
     * @param pSize lado como fracción del alto de la imagen
     */
    public static BufferedImage withSquare(float pX, float pY, float pSize) {
        BufferedImage image = blank();
        int side = Math.round(pSize * TEST_FRAME_HEIGHT);
        int left = Math.round(pX * TEST_FRAME_WIDTH) - side / 2;
        int top = Math.round(pY * TEST_FRAME_HEIGHT) - side / 2;
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(left, top, side, side);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Fondo negro con un solo píxel blanco. */
    public static BufferedImage withPixel(int pX, int pY) {
        BufferedImage image = blank();
        image.setRGB(pX, pY, Color.WHITE.getRGB());
        return image;
    }

    private static BufferedImage paint(Color pColor) {
        BufferedImage image = new BufferedImage(TEST_FRAME_WIDTH, TEST_FRAME_HEIGHT,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(pColor);
            graphics.fillRect(0, 0, TEST_FRAME_WIDTH, TEST_FRAME_HEIGHT);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
