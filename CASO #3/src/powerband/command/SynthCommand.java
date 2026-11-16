package powerband.command;

import static powerband.util.IConstants.ERR_NULL_CLIENT;
import static powerband.util.IConstants.ERR_NULL_EVENT;
import static powerband.util.IConstants.ERR_WRONG_ACTION;
import static powerband.util.IConstants.ERR_WRONG_SYNTH;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import powerband.exception.SynthConnectionException;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.model.SynthType;
import powerband.sc.OscClient;

/**
 * Base de los comandos de synth. Maneja los listeners y define el template method
 * {@link #execute(SynthEvent)}: validar, estampar el tiempo, enviar y notificar.
 */
public abstract class SynthCommand implements ISynthCommand {

    private final OscClient client;
    private final List<ISynthEventListener> listeners = new CopyOnWriteArrayList<>();

    protected SynthCommand(OscClient pClient) {
        if (pClient == null) {
            throw new IllegalArgumentException(ERR_NULL_CLIENT);
        }
        client = pClient;
    }

    @Override
    public final void execute(SynthEvent pEvent) throws SynthConnectionException {
        if (pEvent == null) {
            throw new IllegalArgumentException(ERR_NULL_EVENT);
        }
        validate(pEvent);
        SynthEvent stamped = pEvent.withTimeMs(System.currentTimeMillis());
        if (send(stamped)) {
            notifyListeners(stamped);
        }
    }

    @Override
    public void addListener(ISynthEventListener pListener) {
        if (pListener != null) {
            listeners.add(pListener);
        }
    }

    @Override
    public void removeListener(ISynthEventListener pListener) {
        listeners.remove(pListener);
    }

    /** Lanza {@link IllegalArgumentException} si el evento no corresponde a este comando. */
    protected abstract void validate(SynthEvent pEvent);

    /** Envía el evento a SuperCollider. Devuelve {@code true} si se envió algo. */
    protected abstract boolean send(SynthEvent pEvent) throws SynthConnectionException;

    protected OscClient getClient() {
        return client;
    }

    protected static void requireSynth(SynthEvent pEvent, SynthType pExpected) {
        if (pEvent.getSynth() != pExpected) {
            throw new IllegalArgumentException(ERR_WRONG_SYNTH + pEvent.getSynth());
        }
    }

    protected static void requireAction(SynthEvent pEvent, SynthAction... pAllowed) {
        for (SynthAction allowed : pAllowed) {
            if (pEvent.getAction() == allowed) {
                return;
            }
        }
        throw new IllegalArgumentException(ERR_WRONG_ACTION + pEvent.getAction());
    }

    private void notifyListeners(SynthEvent pEvent) {
        for (ISynthEventListener listener : listeners) {
            listener.onSynthEvent(pEvent);
        }
    }
}
