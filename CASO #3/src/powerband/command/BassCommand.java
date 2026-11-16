package powerband.command;

import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_DUR_MS;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.SYNTH_BASS;

import powerband.exception.SynthConnectionException;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;
import powerband.sc.OscClient;

/**
 * Dispara una nota de {@code stringBass}. El synth termina solo.
 */
public class BassCommand extends SynthCommand {

    public BassCommand(OscClient pClient) {
        super(pClient);
    }

    @Override
    protected void validate(SynthEvent pEvent) {
        requireSynth(pEvent, SynthType.BASS);
        requireAction(pEvent, SynthAction.TRIGGER);
    }

    @Override
    protected boolean send(SynthEvent pEvent) throws SynthConnectionException {
        OscClient client = getClient();
        client.sNew(SYNTH_BASS, client.nextNodeId(),
                PARAM_NOTE, pEvent.getNote(),
                PARAM_DUR_MS, pEvent.getDurationMs(),
                PARAM_AMP, pEvent.getAmp());
        return true;
    }
}
