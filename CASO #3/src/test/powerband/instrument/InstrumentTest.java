package powerband.instrument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import powerband.instrument.helpers.FakeInstrument;
import powerband.instrument.helpers.RecordingCommand;
import powerband.model.SynthEvent;

class InstrumentTest {

    @Test
    void playDelegatesEventToItsCommand() throws Exception {
        RecordingCommand command = new RecordingCommand();
        FakeInstrument instrument = new FakeInstrument(command);
        SynthEvent event = SynthEvent.piano(60, 1000, 4);

        instrument.playEvent(event);

        assertEquals(1, command.getEvents().size());
        assertSame(event, command.getEvents().get(0));
    }

    @Test
    void setCommandRedirectsNextEventsWithoutChangingTheInstrument() throws Exception {
        RecordingCommand first = new RecordingCommand();
        RecordingCommand second = new RecordingCommand();
        FakeInstrument instrument = new FakeInstrument(first);

        instrument.playEvent(SynthEvent.piano(60, 1000, 4));
        instrument.setCommand(second);
        instrument.playEvent(SynthEvent.bass(36, 500));

        assertSame(second, instrument.getCommand());
        assertEquals(1, first.getEvents().size());
        assertEquals(1, second.getEvents().size());
    }

    @Test
    void rejectsNullCommand() {
        assertThrows(IllegalArgumentException.class, () -> new FakeInstrument(null));
        FakeInstrument instrument = new FakeInstrument(new RecordingCommand());
        assertThrows(IllegalArgumentException.class, () -> instrument.setCommand(null));
        assertTrue(instrument.getCommand() instanceof RecordingCommand);
    }
}
