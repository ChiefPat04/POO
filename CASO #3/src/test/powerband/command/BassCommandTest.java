package powerband.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.ADD_TO_HEAD;
import static powerband.util.IConstants.DEFAULT_BASS_AMP;
import static powerband.util.IConstants.DEFAULT_GROUP;
import static powerband.util.IConstants.OSC_S_NEW;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_DUR_MS;
import static powerband.util.IConstants.PARAM_NOTE;
import static powerband.util.IConstants.SC_HOST;
import static powerband.util.IConstants.SYNTH_BASS;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import powerband.command.helpers.OscMessage;
import powerband.command.helpers.OscReceiver;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;

class BassCommandTest {

    @Test
    void sendsSNewStringBassWithNoteDurationAndAmp() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            BassCommand command = new BassCommand(client);
            command.addListener(notified::add);

            command.execute(SynthEvent.bass(36, 450));

            OscMessage message = receiver.receive();
            assertEquals(OSC_S_NEW, message.getPath());
            assertEquals(10, message.getArgs().size());
            assertEquals(SYNTH_BASS, message.arg(0));
            assertEquals(ADD_TO_HEAD, message.arg(2));
            assertEquals(DEFAULT_GROUP, message.arg(3));
            assertEquals(PARAM_NOTE, message.arg(4));
            assertEquals(36, message.arg(5));
            assertEquals(PARAM_DUR_MS, message.arg(6));
            assertEquals(450, message.arg(7));
            assertEquals(PARAM_AMP, message.arg(8));
            assertEquals(DEFAULT_BASS_AMP, (Float) message.arg(9), 1e-6f);
        }
        assertEquals(1, notified.size());
    }

    @Test
    void rejectsEventOfAnotherSynth() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            BassCommand command = new BassCommand(client);
            command.addListener(notified::add);

            assertThrows(IllegalArgumentException.class, () -> command.execute(SynthEvent.piano(48, 1500, 4)));

            assertNull(receiver.receive());
        }
        assertTrue(notified.isEmpty());
    }
}
