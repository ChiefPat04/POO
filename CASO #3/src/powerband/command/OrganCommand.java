package powerband.command;

import static powerband.util.IConstants.FREQ_A4;
import static powerband.util.IConstants.FREQ_BASE;
import static powerband.util.IConstants.GATE_OFF;
import static powerband.util.IConstants.MIDI_A4;
import static powerband.util.IConstants.PARAM_A1;
import static powerband.util.IConstants.PARAM_A2;
import static powerband.util.IConstants.PARAM_A3;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_FREQ;
import static powerband.util.IConstants.PARAM_GATE;
import static powerband.util.IConstants.PARAM_R1;
import static powerband.util.IConstants.PARAM_R2;
import static powerband.util.IConstants.PARAM_R3;
import static powerband.util.IConstants.SEMITONES_PER_OCTAVE;
import static powerband.util.IConstants.SYNTH_ORGAN;

import java.util.HashMap;
import java.util.Map;

import powerband.exception.SynthConnectionException;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;
import powerband.sc.OscClient;

/**
 * Enciende y apaga notas sostenidas de {@code organ70s}.
 *
 * <p>Guarda qué nodo suena para cada nota MIDI. {@link #noteOn} y {@link #noteOff}
 * son {@code synchronized} porque el reinicio (buscar el nodo viejo, enviarle
 * {@code gate 0} y registrar el nuevo) tiene que ser atómico cuando tocan varios hilos.</p>
 */
public class OrganCommand extends SynthCommand {

    private final Map<Integer, Integer> activeNodes = new HashMap<>(); // nota MIDI -> nodeId

    public OrganCommand(OscClient pClient) {
        super(pClient);
    }

    /** Frecuencia en Hz de una nota MIDI: 440 * 2^((nota - 69) / 12). */
    public static float midiToFreq(int pNote) {
        return (float) (FREQ_A4 * Math.pow(FREQ_BASE, (pNote - MIDI_A4) / SEMITONES_PER_OCTAVE));
    }

    /**
     * Apaga todas las notas encendidas. Cada una pasa por {@link #execute} para que
     * los listeners (el grabador) reciban su noteOff.
     */
    public void allNotesOff() throws SynthConnectionException {
        Integer[] notes;
        synchronized (this) {
            notes = activeNodes.keySet().toArray(new Integer[activeNodes.size()]);
        }
        for (Integer note : notes) {
            execute(SynthEvent.organOff(note));
        }
    }

    @Override
    protected void validate(SynthEvent pEvent) {
        requireSynth(pEvent, SynthType.ORGAN);
        requireAction(pEvent, SynthAction.NOTE_ON, SynthAction.NOTE_OFF);
    }

    @Override
    protected boolean send(SynthEvent pEvent) throws SynthConnectionException {
        if (pEvent.getAction() == SynthAction.NOTE_ON) {
            return noteOn(pEvent);
        }
        return noteOff(pEvent.getNote());
    }

    private synchronized boolean noteOn(SynthEvent pEvent) throws SynthConnectionException {
        releaseNode(pEvent.getNote()); // reinicio seguro, igual que BandClient
        OscClient client = getClient();
        int nodeId = client.nextNodeId();
        client.sNew(SYNTH_ORGAN, nodeId,
                PARAM_FREQ, midiToFreq(pEvent.getNote()),
                PARAM_AMP, pEvent.getAmp(),
                PARAM_R1, pEvent.getR1(),
                PARAM_R2, pEvent.getR2(),
                PARAM_R3, pEvent.getR3(),
                PARAM_A1, pEvent.getA1(),
                PARAM_A2, pEvent.getA2(),
                PARAM_A3, pEvent.getA3());
        activeNodes.put(pEvent.getNote(), nodeId);
        return true;
    }

    private synchronized boolean noteOff(int pNote) throws SynthConnectionException {
        return releaseNode(pNote);
    }

    /** Envía {@code gate 0} al nodo de la nota si está sonando. Se llama con el lock tomado. */
    private boolean releaseNode(int pNote) throws SynthConnectionException {
        Integer nodeId = activeNodes.remove(pNote);
        if (nodeId == null) {
            return false;
        }
        getClient().nSet(nodeId, PARAM_GATE, GATE_OFF);
        return true;
    }
}
