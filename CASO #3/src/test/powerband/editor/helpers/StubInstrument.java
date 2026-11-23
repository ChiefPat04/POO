package powerband.editor.helpers;

import powerband.command.ISynthCommand;
import powerband.exception.DeviceException;
import powerband.instrument.Instrument;

/**
 * Instrumento sin dispositivo que puede fallar al iniciar o al detenerse.
 */
public class StubInstrument extends Instrument {

    public static final String START_FAILURE = "dispositivo falso sin conectar";
    public static final String STOP_FAILURE = "dispositivo falso no se detuvo";

    private volatile boolean failOnStart;
    private volatile boolean failOnStop;
    private volatile boolean started;
    private volatile boolean stopped;

    public StubInstrument(ISynthCommand pCommand) {
        super(pCommand);
    }

    @Override
    public void start() throws DeviceException {
        if (failOnStart) {
            throw new DeviceException(START_FAILURE);
        }
        started = true;
    }

    @Override
    public void stop() {
        stopped = true;
        if (failOnStop) {
            throw new IllegalStateException(STOP_FAILURE);
        }
    }

    public void setFailOnStart(boolean pFail) {
        failOnStart = pFail;
    }

    public void setFailOnStop(boolean pFail) {
        failOnStop = pFail;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isStopped() {
        return stopped;
    }
}
