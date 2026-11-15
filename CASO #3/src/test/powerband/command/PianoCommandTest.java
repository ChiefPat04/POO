package powerband.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ADD_TO_HEAD;
import static powerband.util.IConstants.DEFAULT_GROUP;
import static powerband.util.IConstants.DEFAULT_PIANO_AMP;
import static powerband.util.IConstants.OSC_S_NEW;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_DUR_MS;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.PARAM_THIRD;
import static powerband.util.IConstants.SC_HOST;
import static powerband.util.IConstants.SYNTH_PIANO;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import powerband.command.helpers.OscMessage;
import powerband.command.helpers.OscReceiver;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;

class PianoCommandTest {

    @Test
    void sendsSNewClassicPianoWithNoteDurationThirdAndAmp() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            PianoCommand command = new PianoCommand(client);
            command.addListener(notified::add);

            command.execute(SynthEvent.piano(48, 1500, 3));

            OscMessage message = receiver.receive();
            assertEquals(OSC_S_NEW, message.getPath());
            assertEquals(12, message.getArgs().size());
            assertEquals(SYNTH_PIANO, message.arg(0));
            assertTrue(message.arg(1) instanceof Integer);
            assertEquals(ADD_TO_HEAD, message.arg(2));
            assertEquals(DEFAULT_GROUP, message.arg(3));
            assertEquals(PARAM_NOTE, message.arg(4));
            assertEquals(48, message.arg(5));
            assertEquals(PARAM_DUR_MS, message.arg(6));
            assertEquals(1500, message.arg(7));
            assertEquals(PARAM_THIRD, message.arg(8));
            assertEquals(3, message.arg(9));
            assertEquals(PARAM_AMP, message.arg(10));
            assertEquals(DEFAULT_PIANO_AMP, (Float) message.arg(11), 1e-6f);
        }

        assertEquals(1, notified.size());
        assertTrue(notified.get(0).getTimeMs() > 0);
    }

    @Test
    void rejectsEventOfAnotherSynth() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            PianoCommand command = new PianoCommand(client);
            command.addListener(notified::add);

            assertThrows(IllegalArgumentException.class, () -> command.execute(SynthEvent.bass(36, 450)));
            assertThrows(IllegalArgumentException.class, () -> command.execute(SynthEvent.organOn(60)));

            assertNull(receiver.receive());
        }
        assertTrue(notified.isEmpty());
    }
}
