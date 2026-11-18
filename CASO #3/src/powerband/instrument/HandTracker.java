package powerband.instrument;

import static powerband.util.IConstants.CAM_GRAY_BAND;
import static powerband.util.IConstants.CAM_MAX_MOTION;
import static powerband.util.IConstants.CAM_MIN_MOTION;
import static powerband.util.IConstants.CAM_MIRROR;
import static powerband.util.IConstants.CAM_NORMALIZED_MAX;
import static powerband.util.IConstants.CAM_PIXEL_CENTER;
import static powerband.util.IConstants.CAM_PIXEL_THRESHOLD;
import static powerband.util.IConstants.FIRST_INDEX;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;

import powerband.model.HandReading;

/**
 * Encuentra la mano comparando cada cuadro con el anterior: reduce la imagen a una
 * cuadrícula en gris, marca los píxeles que cambiaron y toma su centro.
 *
 * <p>Solo ve lo que se mueve: el cuerpo quieto no cuenta. Si se mueve todo el
 * cuerpo, el centro se corre.</p>
 */
public class HandTracker {

    private final int gridWidth;
    private final int gridHeight;
    private int[] previous;

    public HandTracker(int pGridWidth, int pGridHeight) {
        gridWidth = pGridWidth;
        gridHeight = pGridHeight;
    }

    /**
     * Compara el cuadro con el anterior y dice dónde se movió la mano.
     *
     * @return {@link HandReading#absent()} si es el primer cuadro, si casi nada
     *         cambió o si cambió casi toda la imagen (luz, cámara)
     */
    public HandReading track(BufferedImage pFrame) {
        int[] current = toGrid(pFrame);
        int[] last = previous;
        previous = current;
        if (last == null) {
            return HandReading.absent();
        }

        int changed = FIRST_INDEX;
        long sumColumns = FIRST_INDEX;
        long sumRows = FIRST_INDEX;
        for (int row = FIRST_INDEX; row < gridHeight; row++) {
            for (int column = FIRST_INDEX; column < gridWidth; column++) {
                int index = row * gridWidth + column;
                if (Math.abs(current[index] - last[index]) >= CAM_PIXEL_THRESHOLD) {
                    changed++;
                    sumColumns += column;
                    sumRows += row;
                }
            }
        }

        float motion = (float) changed / current.length;
        if (motion < CAM_MIN_MOTION || motion > CAM_MAX_MOTION) {
            return HandReading.absent();
        }
        float x = ((float) sumColumns / changed + CAM_PIXEL_CENTER) / gridWidth;
        float y = ((float) sumRows / changed + CAM_PIXEL_CENTER) / gridHeight;
        if (CAM_MIRROR) {
            x = CAM_NORMALIZED_MAX - x;
        }
        return new HandReading(true, x, y, motion);
    }

    /** Olvida el cuadro anterior: el siguiente vuelve a dar ausente. */
    public void reset() {
        previous = null;
    }

    // ------------------------------------------------------------ auxiliares

    /** Escala el cuadro a la cuadrícula y lo pasa a gris en un solo paso. */
    private int[] toGrid(BufferedImage pFrame) {
        BufferedImage gray = new BufferedImage(gridWidth, gridHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = gray.createGraphics();
        try {
            graphics.drawImage(pFrame, FIRST_INDEX, FIRST_INDEX, gridWidth, gridHeight, null);
        } finally {
            graphics.dispose();
        }
        Raster raster = gray.getRaster();
        int[] values = new int[gridWidth * gridHeight];
        for (int row = FIRST_INDEX; row < gridHeight; row++) {
            for (int column = FIRST_INDEX; column < gridWidth; column++) {
                values[row * gridWidth + column] = raster.getSample(column, row, CAM_GRAY_BAND);
            }
        }
        return values;
    }
}
