package powerband.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static powerband.util.IConstants.SC_HOST;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import powerband.command.helpers.OscReceiver;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;

class SynthCommandListenerTest {

    @Test
    void everyRegisteredListenerReceivesEachEventAndRemovedListenerStops() throws Exception {
        List<SynthEvent> first = new ArrayList<>();
        List<SynthEvent> second = new ArrayList<>();
        ISynthEventListener firstListener = first::add;
        ISynthEventListener secondListener = second::add;

        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            BassCommand command = new BassCommand(client);
            command.addListener(firstListener);
            command.addListener(secondListener);

            command.execute(SynthEvent.bass(36, 450));
            command.execute(SynthEvent.bass(43, 450));
            command.removeListener(secondListener);
            command.execute(SynthEvent.bass(45, 450));

            assertEquals(3, receiver.receiveAll().size());
        }

        assertEquals(3, first.size());
        assertEquals(2, second.size());
        assertEquals(45, first.get(2).getNote());
    }
}
