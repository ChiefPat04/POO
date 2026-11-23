package powerband;

import static powerband.util.IConstants.DIALOG_ERROR_TITLE;
import static powerband.util.IConstants.ERR_SC_CONNECT;
import static powerband.util.IConstants.EXIT_OK;
import static powerband.util.IConstants.LABEL_GAMEPAD;
import static powerband.util.IConstants.LABEL_MICROPHONE;
import static powerband.util.IConstants.LABEL_WEBCAM;
import static powerband.util.IConstants.SC_HOST;
import static powerband.util.IConstants.SC_PORT;
import static powerband.util.IConstants.WINDOW_CLOSE_SIGNALS;

import java.util.concurrent.CountDownLatch;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import powerband.command.BassCommand;
import powerband.command.OrganCommand;
import powerband.command.PianoCommand;
import powerband.editor.EditorController;
import powerband.editor.Player;
import powerband.editor.Recorder;
import powerband.editor.RecordingLibrary;
import powerband.exception.SynthConnectionException;
import powerband.instrument.GamepadInstrument;
import powerband.instrument.JamepadReader;
import powerband.instrument.MicrophoneInstrument;
import powerband.instrument.MicrophoneSource;
import powerband.instrument.WebcamInstrument;
import powerband.instrument.WebcamSource;
import powerband.sc.OscClient;
import powerband.storage.RecordingIndex;
import powerband.storage.RecordingStorage;
import powerband.ui.EditorWindow;

/**
 * Arma Power Band: conexión con SuperCollider, comandos, grabación, reproducción,
 * los 3 instrumentos y la ventana del editor. Espera a que se cierre la ventana y
 * entonces cierra la conexión.
 */
public class PowerBandMain {

    public static void main(String[] pArgs) {
        try (OscClient client = new OscClient(SC_HOST, SC_PORT)) {
            PianoCommand piano = new PianoCommand(client);
            BassCommand bass = new BassCommand(client);
            OrganCommand organ = new OrganCommand(client);

            RecordingStorage storage = new RecordingStorage();
            RecordingIndex index = new RecordingIndex();
            Recorder recorder = new Recorder(storage, index, piano, bass, organ);
            Player player = new Player(storage, piano, bass, organ);
            RecordingLibrary library = new RecordingLibrary(storage, index);
            EditorController controller = new EditorController(recorder, player, library, organ);

            controller.addInstrument(LABEL_GAMEPAD, new GamepadInstrument(bass, new JamepadReader()));
            controller.addInstrument(LABEL_MICROPHONE,
                    new MicrophoneInstrument(organ, new MicrophoneSource()));
            controller.addInstrument(LABEL_WEBCAM, new WebcamInstrument(piano, new WebcamSource()));
            controller.startInstruments();

            CountDownLatch closed = new CountDownLatch(WINDOW_CLOSE_SIGNALS);
            SwingUtilities.invokeLater(
                    () -> new EditorWindow(controller, closed, piano, bass, organ).setVisible(true));
            awaitQuietly(closed);
        } catch (SynthConnectionException e) {
            JOptionPane.showMessageDialog(null, ERR_SC_CONNECT + e.getMessage(),
                    DIALOG_ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        }
        System.exit(EXIT_OK); // termina los hilos de las librerías de dispositivos
    }

    private static void awaitQuietly(CountDownLatch pLatch) {
        try {
            pLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
