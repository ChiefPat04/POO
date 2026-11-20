package powerband.editor.helpers;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import powerband.command.ISynthCommand;
import powerband.command.ISynthEventListener;
import powerband.exception.SynthConnectionException;
import powerband.model.SynthEvent;

/**
 * {@link ISynthCommand} falso que guarda cada evento con la hora en que llegó.
 */
public class TimedCommand implements ISynthCommand {

    private final List<SynthEvent> events = new CopyOnWriteArrayList<>();
    private final List<Long> times = new CopyOnWriteArrayList<>();
    private volatile boolean failing;

    @Override
    public void execute(SynthEvent pEvent) throws SynthConnectionException {
        if (failing) {
            throw new SynthConnectionException("comando falso sin conexión");
        }
        times.add(System.currentTimeMillis());
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

    public List<Long> getTimes() {
        return times;
    }

    public void setFailing(boolean pFail) {
        failing = pFail;
    }
}
