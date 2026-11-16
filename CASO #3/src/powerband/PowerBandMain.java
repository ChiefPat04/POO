package powerband;

import static powerband.util.IConstants.APP_NAME;
import static powerband.util.IConstants.ERR_CONSOLE_READ;
import static powerband.util.IConstants.MSG_PRESS_ENTER_TO_EXIT;
import static powerband.util.IConstants.SC_HOST;
import static powerband.util.IConstants.SC_PORT;

import java.io.IOException;

import powerband.command.BassCommand;
import powerband.command.OrganCommand;
import powerband.command.PianoCommand;
import powerband.exception.PowerBandException;
import powerband.instrument.GamepadInstrument;
import powerband.instrument.JamepadReader;
import powerband.sc.OscClient;

/**
 * Entrada provisional (la reemplaza la ventana del editor, ft-8): conecta con
 * SuperCollider y deja tocar el bajo con el control hasta que se presione Enter.
 */
public class PowerBandMain {

    public static void main(String[] pArgs) {
        System.out.println(APP_NAME);
        try (OscClient client = new OscClient(SC_HOST, SC_PORT)) {
            PianoCommand piano = new PianoCommand(client); // todavía sin instrumento (ft-4)
            BassCommand bass = new BassCommand(client);
            OrganCommand organ = new OrganCommand(client);

            GamepadInstrument gamepad = new GamepadInstrument(bass, new JamepadReader());
            gamepad.start();
            try {
                System.out.println(MSG_PRESS_ENTER_TO_EXIT);
                System.in.read();
            } finally {
                gamepad.stop();
                organ.allNotesOff();
            }
        } catch (PowerBandException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(ERR_CONSOLE_READ + e.getMessage());
        }
    }
}
