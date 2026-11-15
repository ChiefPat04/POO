package powerband.instrument.helpers;

import java.util.ArrayList;
import java.util.List;

import powerband.command.ISynthCommand;
import powerband.command.ISynthEventListener;
import powerband.model.SynthEvent;

/**
 * {@link ISynthCommand} falso que solo guarda los eventos que recibe.
 */
public class RecordingCommand implements ISynthCommand {

    private final List<SynthEvent> events = new ArrayList<>();

    @Override
    public void execute(SynthEvent pEvent) {
        events.add(pEvent);
    }

    @Override
    public void addListener(ISynthEventListener pListener) {
        // este comando falso no notifica a nadie
    }

    @Override
    public void removeListener(ISynthEventListener pListener) {
        // este comando falso no notifica a nadie
    }

    public List<SynthEvent> getEvents() {
        return events;
    }
}
