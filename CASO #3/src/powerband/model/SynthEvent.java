package powerband.model;

import static powerband.util.IConstants.DEFAULT_BASS_AMP;
import static powerband.util.IConstants.DEFAULT_ORGAN_AMP;
import static powerband.util.IConstants.DEFAULT_ORGAN_AMPS;
import static powerband.util.IConstants.DEFAULT_ORGAN_RATIOS;
import static powerband.util.IConstants.DEFAULT_PIANO_AMP;
import static powerband.util.IConstants.ERR_ORGAN_PARTIALS;
import static powerband.util.IConstants.FIRST_PARTIAL;
import static powerband.util.IConstants.ORGAN_PARTIALS;
import static powerband.util.IConstants.SECOND_PARTIAL;
import static powerband.util.IConstants.THIRD_PARTIAL;
import static powerband.util.IConstants.UNUSED_FLOAT;
import static powerband.util.IConstants.UNUSED_INT;

import java.util.List;

/**
 * Comando musical enviado a un synth. Es inmutable: para cambiar el tiempo se usa
 * {@link #withTimeMs(long)}, que devuelve un evento nuevo.
 *
 * <p>Se crea solo con las fábricas estáticas ({@link #piano}, {@link #bass},
 * {@link #organOn}, {@link #organOff}), que garantizan que el synth y la acción
 * sean coherentes. Los campos que no aplican al synth quedan en
 * {@code UNUSED_INT} / {@code UNUSED_FLOAT}.</p>
 *
 * <p>{@code timeMs} es la hora absoluta de envío cuando lo estampa un comando;
 * el grabador (ft-5) lo convierte a tiempo relativo al inicio de la grabación.</p>
 */
public final class SynthEvent {

    private final long timeMs;
    private final SynthType synth;
    private final SynthAction action;
    private final int note;
    private final int durationMs;
    private final int third;
    private final float amp;
    private final float r1;
    private final float r2;
    private final float r3;
    private final float a1;
    private final float a2;
    private final float a3;

    private SynthEvent(long pTimeMs, SynthType pSynth, SynthAction pAction, int pNote,
            int pDurationMs, int pThird, float pAmp, float[] pRatios, float[] pAmps) {
        timeMs = pTimeMs;
        synth = pSynth;
        action = pAction;
        note = pNote;
        durationMs = pDurationMs;
        third = pThird;
        amp = pAmp;
        r1 = pRatios[FIRST_PARTIAL];
        r2 = pRatios[SECOND_PARTIAL];
        r3 = pRatios[THIRD_PARTIAL];
        a1 = pAmps[FIRST_PARTIAL];
        a2 = pAmps[SECOND_PARTIAL];
        a3 = pAmps[THIRD_PARTIAL];
    }

    // ------------------------------------------------------------ fábricas

    public static SynthEvent piano(int pNote, int pDurationMs, int pThird, float pAmp) {
        return new SynthEvent(UNUSED_INT, SynthType.PIANO, SynthAction.TRIGGER, pNote,
                pDurationMs, pThird, pAmp, unusedPartials(), unusedPartials());
    }

    public static SynthEvent piano(int pNote, int pDurationMs, int pThird) {
        return piano(pNote, pDurationMs, pThird, DEFAULT_PIANO_AMP);
    }

    public static SynthEvent bass(int pNote, int pDurationMs, float pAmp) {
        return new SynthEvent(UNUSED_INT, SynthType.BASS, SynthAction.TRIGGER, pNote,
                pDurationMs, UNUSED_INT, pAmp, unusedPartials(), unusedPartials());
    }

    public static SynthEvent bass(int pNote, int pDurationMs) {
        return bass(pNote, pDurationMs, DEFAULT_BASS_AMP);
    }

    /**
     * Enciende una nota del órgano.
     *
     * @throws IllegalArgumentException si los arreglos no miden {@code ORGAN_PARTIALS}
     */
    public static SynthEvent organOn(int pNote, float pAmp, float[] pRatios, float[] pAmps) {
        if (pRatios == null || pAmps == null
                || pRatios.length != ORGAN_PARTIALS || pAmps.length != ORGAN_PARTIALS) {
            throw new IllegalArgumentException(ERR_ORGAN_PARTIALS);
        }
        return new SynthEvent(UNUSED_INT, SynthType.ORGAN, SynthAction.NOTE_ON, pNote,
                UNUSED_INT, UNUSED_INT, pAmp, pRatios, pAmps);
    }

    public static SynthEvent organOn(int pNote) {
        return organOn(pNote, DEFAULT_ORGAN_AMP,
                toArray(DEFAULT_ORGAN_RATIOS), toArray(DEFAULT_ORGAN_AMPS));
    }

    public static SynthEvent organOff(int pNote) {
        return new SynthEvent(UNUSED_INT, SynthType.ORGAN, SynthAction.NOTE_OFF, pNote,
                UNUSED_INT, UNUSED_INT, UNUSED_FLOAT, unusedPartials(), unusedPartials());
    }

    // ------------------------------------------------------------ tiempo

    /** Devuelve una copia del evento con otro tiempo; este evento no cambia. */
    public SynthEvent withTimeMs(long pTimeMs) {
        return new SynthEvent(pTimeMs, synth, action, note, durationMs, third, amp,
                new float[] { r1, r2, r3 }, new float[] { a1, a2, a3 });
    }

    // ------------------------------------------------------------ getters

    public long getTimeMs() {
        return timeMs;
    }

    public SynthType getSynth() {
        return synth;
    }

    public SynthAction getAction() {
        return action;
    }

    public int getNote() {
        return note;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public int getThird() {
        return third;
    }

    public float getAmp() {
        return amp;
    }

    public float getR1() {
        return r1;
    }

    public float getR2() {
        return r2;
    }

    public float getR3() {
        return r3;
    }

    public float getA1() {
        return a1;
    }

    public float getA2() {
        return a2;
    }

    public float getA3() {
        return a3;
    }

    // ------------------------------------------------------------ auxiliares

    private static float[] unusedPartials() {
        float[] partials = new float[ORGAN_PARTIALS];
        for (int i = FIRST_PARTIAL; i < ORGAN_PARTIALS; i++) {
            partials[i] = UNUSED_FLOAT;
        }
        return partials;
    }

    private static float[] toArray(List<Float> pValues) {
        float[] values = new float[pValues.size()];
        for (int i = FIRST_PARTIAL; i < values.length; i++) {
            values[i] = pValues.get(i);
        }
        return values;
    }
}
