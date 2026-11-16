package powerband.instrument.helpers;

import powerband.command.ISynthCommand;
import powerband.exception.SynthConnectionException;
import powerband.instrument.Instrument;
import powerband.model.SynthEvent;

/**
 * Instrumento mínimo sin dispositivo: expone {@code play} para las pruebas.
 */
public class FakeInstrument extends Instrument {

    public FakeInstrument(ISynthCommand pCommand) {
        super(pCommand);
    }

    @Override
    public void start() {
        // sin dispositivo que leer
    }

    @Override
    public void stop() {
        // sin dispositivo que leer
    }

    public void playEvent(SynthEvent pEvent) throws SynthConnectionException {
        play(pEvent);
    }
}
