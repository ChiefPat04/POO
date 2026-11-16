package powerband.instrument.helpers;

import static powerband.util.IConstants.AUDIO_BLOCK_SIZE;
import static powerband.util.IConstants.AUDIO_SAMPLE_RATE;

import java.util.Random;

/**
 * Señales sintéticas para probar el micrófono sin micrófono. Cada método
 * devuelve un bloque de {@code AUDIO_BLOCK_SIZE} muestras a {@code AUDIO_SAMPLE_RATE}.
 */
public final class TestSignals {

    private TestSignals() {
    }

    public static float[] sine(float pFrequency, float pAmplitude) {
        float[] block = new float[AUDIO_BLOCK_SIZE];
        for (int i = 0; i < block.length; i++) {
            block[i] = (float) (pAmplitude * Math.sin(phase(pFrequency, i)));
        }
        return block;
    }

    /** Fundamental + 2.º armónico a 0.5 + 3.º armónico a 0.25. */
    public static float[] harmonics(float pFrequency, float pAmplitude) {
        float[] block = new float[AUDIO_BLOCK_SIZE];
        for (int i = 0; i < block.length; i++) {
            block[i] = (float) (pAmplitude * (Math.sin(phase(pFrequency, i))
                    + 0.5 * Math.sin(phase(2 * pFrequency, i))
                    + 0.25 * Math.sin(phase(3 * pFrequency, i))));
        }
        return block;
    }

    public static float[] silence() {
        return new float[AUDIO_BLOCK_SIZE];
    }

    public static float[] noise(long pSeed, float pAmplitude) {
        Random random = new Random(pSeed);
        float[] block = new float[AUDIO_BLOCK_SIZE];
        for (int i = 0; i < block.length; i++) {
            block[i] = (random.nextFloat() * 2f - 1f) * pAmplitude;
        }
        return block;
    }

    /** Frecuencia exacta de una nota MIDI (La4 = 69 = 440 Hz). */
    public static float noteFrequency(int pNote) {
        return (float) (440.0 * Math.pow(2.0, (pNote - 69) / 12.0));
    }

    private static double phase(float pFrequency, int pIndex) {
        return 2.0 * Math.PI * pFrequency * pIndex / AUDIO_SAMPLE_RATE;
    }
}
