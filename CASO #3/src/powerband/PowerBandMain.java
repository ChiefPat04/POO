package powerband;

import static powerband.util.IConstants.APP_NAME;
import static powerband.util.IConstants.ERR_CONSOLE_READ;
import static powerband.util.IConstants.ERR_INSTRUMENT_START;
import static powerband.util.IConstants.MSG_NO_INSTRUMENTS;
import static powerband.util.IConstants.MSG_PRESS_ENTER_TO_EXIT;
import static powerband.util.IConstants.SC_HOST;
import static powerband.util.IConstants.SC_PORT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import powerband.command.BassCommand;
import powerband.command.OrganCommand;
import powerband.command.PianoCommand;
import powerband.exception.PowerBandException;
import powerband.instrument.GamepadInstrument;
import powerband.instrument.Instrument;
import powerband.instrument.JamepadReader;
import powerband.instrument.MicrophoneInstrument;
import powerband.instrument.MicrophoneSource;
import powerband.instrument.WebcamInstrument;
import powerband.instrument.WebcamSource;
import powerband.sc.OscClient;

/**
 * Entrada provisional (la reemplaza la ventana del editor, ft-8): conecta con
 * SuperCollider e inicia el control (bajo), el micrófono (órgano) y la webcam
 * (piano) hasta que se presione Enter. Si un instrumento no inicia, se informa y
 * los demás siguen.
 */
public class PowerBandMain {

    public static void main(String[] pArgs) {
        System.out.println(APP_NAME);
        try (OscClient client = new OscClient(SC_HOST, SC_PORT)) {
            PianoCommand piano = new PianoCommand(client);
            BassCommand bass = new BassCommand(client);
            OrganCommand organ = new OrganCommand(client);

            List<Instrument> instruments = List.of(
                    new GamepadInstrument(bass, new JamepadReader()),
                    new MicrophoneInstrument(organ, new MicrophoneSource()),
                    new WebcamInstrument(piano, new WebcamSource()));
            List<Instrument> started = new ArrayList<>();
            try {
                for (Instrument instrument : instruments) {
                    try {
                        instrument.start();
                        started.add(instrument);
                    } catch (PowerBandException e) {
                        System.err.println(ERR_INSTRUMENT_START + e.getMessage());
                    }
                }
                if (started.isEmpty()) {
                    System.out.println(MSG_NO_INSTRUMENTS);
                } else {
                    System.out.println(MSG_PRESS_ENTER_TO_EXIT);
                    System.in.read();
                }
            } finally {
                for (Instrument instrument : started) {
                    instrument.stop();
                }
                organ.allNotesOff();
            }
        } catch (PowerBandException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(ERR_CONSOLE_READ + e.getMessage());
        }
    }
}
