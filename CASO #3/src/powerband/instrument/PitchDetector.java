package powerband.instrument;

import static powerband.util.IConstants.FIRST_INDEX;
import static powerband.util.IConstants.FREQ_A4;
import static powerband.util.IConstants.FREQ_BASE;
import static powerband.util.IConstants.MIDI_A4;
import static powerband.util.IConstants.NO_CORRELATION;
import static powerband.util.IConstants.NO_ENERGY;
import static powerband.util.IConstants.NO_PITCH;
import static powerband.util.IConstants.PARABOLA_DOUBLE;
import static powerband.util.IConstants.PARABOLA_HALF;
import static powerband.util.IConstants.PITCH_CLARITY_THRESHOLD;
import static powerband.util.IConstants.PITCH_FIRST_LAG;
import static powerband.util.IConstants.PITCH_LAG_STEP;
import static powerband.util.IConstants.PITCH_MAX_FREQ;
import static powerband.util.IConstants.PITCH_MAX_LAG_DIVISOR;
import static powerband.util.IConstants.PITCH_MIN_ENERGY;
import static powerband.util.IConstants.PITCH_MIN_FREQ;
import static powerband.util.IConstants.SEMITONES_PER_OCTAVE;

/**
 * Detecta la nota de una ventana de audio: frecuencia fundamental por
 * autocorrelación normalizada, volumen RMS y conversión a nota MIDI.
 */
public class PitchDetector {

    private final float sampleRate;

    public PitchDetector(float pSampleRate) {
        sampleRate = pSampleRate;
    }

    /**
     * Frecuencia fundamental en Hz, o {@code NO_PITCH} si la señal no es periódica
     * (silencio, ruido) o queda fuera de {@code PITCH_MIN_FREQ}..{@code PITCH_MAX_FREQ}.
     */
    public float detectFrequency(float[] pSamples) {
        if (meanEnergy(pSamples) < PITCH_MIN_ENERGY) {
            return NO_PITCH;
        }
        int maxLag = Math.min((int) (sampleRate / PITCH_MIN_FREQ),
                pSamples.length / PITCH_MAX_LAG_DIVISOR);

        // 1. Saltar la bajada inicial: cerca del desfase 0 la correlación es alta
        //    sin que haya pasado un período.
        int lag = PITCH_FIRST_LAG;
        while (lag <= maxLag && correlation(pSamples, lag) >= NO_CORRELATION) {
            lag += PITCH_LAG_STEP;
        }

        // 2. Primer máximo local suficientemente claro: es el período. Tomar el
        //    primero evita errores de octava.
        double previous = correlation(pSamples, lag);
        double current = correlation(pSamples, lag + PITCH_LAG_STEP);
        for (lag += PITCH_LAG_STEP; lag < maxLag; lag += PITCH_LAG_STEP) {
            double next = correlation(pSamples, lag + PITCH_LAG_STEP);
            if (current >= PITCH_CLARITY_THRESHOLD && current > previous && current >= next) {
                float frequency = (float) (sampleRate / refineLag(lag, previous, current, next));
                boolean inRange = frequency >= PITCH_MIN_FREQ && frequency <= PITCH_MAX_FREQ;
                return inRange ? frequency : NO_PITCH;
            }
            previous = current;
            current = next;
        }
        return NO_PITCH;
    }

    /** Volumen de la ventana: raíz de la energía media. */
    public float rms(float[] pSamples) {
        return (float) Math.sqrt(meanEnergy(pSamples));
    }

    /** Nota MIDI más cercana a esa frecuencia. */
    public int toMidiNote(float pFrequency) {
        double octaves = Math.log(pFrequency / FREQ_A4) / Math.log(FREQ_BASE);
        return (int) Math.round(MIDI_A4 + SEMITONES_PER_OCTAVE * octaves);
    }

    // ------------------------------------------------------------ auxiliares

    private static double meanEnergy(float[] pSamples) {
        if (pSamples.length == FIRST_INDEX) {
            return NO_ENERGY;
        }
        double energy = NO_ENERGY;
        for (float sample : pSamples) {
            energy += sample * sample;
        }
        return energy / pSamples.length;
    }

    /** Autocorrelación normalizada (-1 a 1) de la ventana contra sí misma desplazada. */
    private static double correlation(float[] pSamples, int pLag) {
        int overlap = pSamples.length - pLag;
        double cross = NO_ENERGY;
        double energyA = NO_ENERGY;
        double energyB = NO_ENERGY;
        for (int i = FIRST_INDEX; i < overlap; i++) {
            double a = pSamples[i];
            double b = pSamples[i + pLag];
            cross += a * b;
            energyA += a * a;
            energyB += b * b;
        }
        double scale = Math.sqrt(energyA * energyB);
        if (overlap <= FIRST_INDEX || scale / overlap < PITCH_MIN_ENERGY) {
            return NO_CORRELATION;
        }
        return cross / scale;
    }

    /**
     * Afina el desfase del máximo con una parábola por los tres valores vecinos.
     * En las notas agudas un desfase entero equivale a casi un semitono.
     */
    private static double refineLag(int pLag, double pPrevious, double pCurrent, double pNext) {
        double curvature = pPrevious - PARABOLA_DOUBLE * pCurrent + pNext;
        if (curvature == NO_CORRELATION) {
            return pLag;
        }
        return pLag + PARABOLA_HALF * (pPrevious - pNext) / curvature;
    }
}
