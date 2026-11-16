package powerband.command;

import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_DUR_MS;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.PARAM_THIRD;
import static powerband.util.IConstants.SYNTH_PIANO;

import powerband.exception.SynthConnectionException;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;
import powerband.sc.OscClient;

/**
 * Dispara un acorde de {@code classicPiano}. El synth termina solo.
 */
public class PianoCommand extends SynthCommand {

    public PianoCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    protected void validate(SynthEvent pEvent) {
        requireSynth(pEvent, SynthType.PIANO);
        requireAction(pEvent, SynthAction.TRIGGER);
    }

    @Override
    protected boolean send(SynthEvent pEvent) throws SynthConnectionException {
        OscClient client = getClient();
        client.sNew(SYNTH_PIANO, client.nextNodeId(),
                PARAM_NOTE, pEvent.getNote(),
                PARAM_DUR_MS, pEvent.getDurationMs(),
                PARAM_THIRD, pEvent.getThird(),
                PARAM_AMP, pEvent.getAmp());
        return true;
    }
}
