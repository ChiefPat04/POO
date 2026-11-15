package powerband.instrument;

import static powerband.util.IConstants.ERR_NULL_COMMAND;

import powerband.command.ISynthCommand;
import powerband.exception.PowerBandException;
import powerband.exception.SynthConnectionException;
import powerband.model.SynthEvent;

/**
 * Abstracción del patrón Bridge: un dispositivo que se toca y produce eventos musicales.
 * No sabe qué synth suena; delega en su {@link ISynthCommand}, que se puede cambiar
 * con {@link #setCommand} sin tocar la subclase.
 *
 * <p>Cada subclase lee su dispositivo en su propio hilo; por eso el comando es
 * {@code volatile}.</p>
 */
public abstract class Instrument {

    private volatile ISynthCommand command;

    protected Instrument(ISynthCommand pCommand) {
        setCommand(pCommand);
    }

    public final void setCommand(ISynthCommand pCommand) {
        if (pCommand == null) {
            throw new IllegalArgumentException(ERR_NULL_COMMAND);
        }
        command = pCommand;
    }

    public ISynthCommand getCommand() {
        return command;
    }

    /** Empieza a leer el dispositivo. */
    public abstract void start() throws PowerBandException;

    /** Deja de leer el dispositivo. */
    public abstract void stop();

    protected void play(SynthEvent pEvent) throws SynthConnectionException {
        command.execute(pEvent);
    }
}
