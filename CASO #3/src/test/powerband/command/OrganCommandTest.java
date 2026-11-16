package powerband.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.GATE_OFF;
import static powerband.util.IConstants.OSC_N_SET;
import static powerband.util.IConstants.OSC_S_NEW;
import static powerband.util.IConstants.PARAM_A1;
import static powerband.util.IConstants.PARAM_A2;
import static powerband.util.IConstants.PARAM_A3;
import static powerband.util.IConstants.PARAM_AMP;
import static powerband.util.IConstants.PARAM_FREQ;
import static powerband.util.IConstants.PARAM_GATE;
import static powerband.util.IConstants.PARAM_R1;
import static powerband.util.IConstants.PARAM_R2;
import static powerband.util.IConstants.PARAM_R3;
import static powerband.util.IConstants.SC_HOST;
import static powerband.util.IConstants.SYNTH_ORGAN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import powerband.command.helpers.OscMessage;
import powerband.command.helpers.OscReceiver;
import powerband.model.SynthAction;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;

class OrganCommandTest {

    private static final float DELTA = 1e-4f;

    @Test
    void noteOnSendsFreqAmpRatiosAndAmps() throws Exception {
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);

            command.execute(SynthEvent.organOn(69, 0.4f, new float[] { 1f, 2.01f, 4f },
                    new float[] { 1f, 0.6f, 0.3f }));

            OscMessage message = receiver.receive();
            assertEquals(OSC_S_NEW, message.getPath());
            assertEquals(SYNTH_ORGAN, message.arg(0));
            assertEquals(20, message.getArgs().size());
            assertEquals(PARAM_FREQ, message.arg(4));
            assertEquals(440.0f, (Float) message.arg(5), DELTA);
            assertEquals(PARAM_AMP, message.arg(6));
            assertEquals(0.4f, (Float) message.arg(7), DELTA);
            assertEquals(PARAM_R1, message.arg(8));
            assertEquals(1f, (Float) message.arg(9), DELTA);
            assertEquals(PARAM_R2, message.arg(10));
            assertEquals(2.01f, (Float) message.arg(11), DELTA);
            assertEquals(PARAM_R3, message.arg(12));
            assertEquals(4f, (Float) message.arg(13), DELTA);
            assertEquals(PARAM_A1, message.arg(14));
            assertEquals(1f, (Float) message.arg(15), DELTA);
            assertEquals(PARAM_A2, message.arg(16));
            assertEquals(0.6f, (Float) message.arg(17), DELTA);
            assertEquals(PARAM_A3, message.arg(18));
            assertEquals(0.3f, (Float) message.arg(19), DELTA);
        }
    }

    @Test
    void midiToFreqFollowsEqualTemperament() {
        assertEquals(440.0f, OrganCommand.midiToFreq(69), DELTA);
        assertEquals(880.0f, OrganCommand.midiToFreq(81), DELTA);
        assertEquals((float) (440.0 * Math.pow(2.0, (60 - 69) / 12.0)), OrganCommand.midiToFreq(60), DELTA);
    }

    @Test
    void noteOffSendsGateZeroToSameNode() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);
            command.addListener(notified::add);

            command.execute(SynthEvent.organOn(60));
            command.execute(SynthEvent.organOff(60));

            OscMessage on = receiver.receive();
            OscMessage off = receiver.receive();
            assertEquals(OSC_S_NEW, on.getPath());
            assertEquals(OSC_N_SET, off.getPath());
            assertEquals(List.of(on.arg(1), PARAM_GATE, GATE_OFF), off.getArgs());
        }
        assertEquals(2, notified.size());
    }

    @Test
    void noteOnOnActiveNoteReleasesPreviousNodeFirst() throws Exception {
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);

            command.execute(SynthEvent.organOn(60));
            command.execute(SynthEvent.organOn(60));

            OscMessage first = receiver.receive();
            OscMessage release = receiver.receive();
            OscMessage second = receiver.receive();
            assertEquals(OSC_S_NEW, first.getPath());
            assertEquals(OSC_N_SET, release.getPath());
            assertEquals(first.arg(1), release.arg(0));
            assertEquals(OSC_S_NEW, second.getPath());
            assertTrue(!first.arg(1).equals(second.arg(1)));
        }
    }

    @Test
    void noteOffOnInactiveNoteSendsNothingAndDoesNotNotify() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);
            command.addListener(notified::add);

            command.execute(SynthEvent.organOff(60));

            assertNull(receiver.receive());
        }
        assertTrue(notified.isEmpty());
    }

    @Test
    void rejectsTriggerAction() throws Exception {
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);

            assertThrows(IllegalArgumentException.class, () -> command.execute(SynthEvent.piano(60, 500, 4)));
            assertThrows(IllegalArgumentException.class, () -> command.execute(SynthEvent.bass(36, 500)));

            assertNull(receiver.receive());
        }
    }

    @Test
    void allNotesOffReleasesEveryActiveNodeAndNotifies() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);
            command.addListener(notified::add);
            command.execute(SynthEvent.organOn(60));
            command.execute(SynthEvent.organOn(64));

            command.allNotesOff();

            List<Object> started = new ArrayList<>();
            List<Object> released = new ArrayList<>();
            for (OscMessage message : receiver.receiveAll()) {
                if (OSC_S_NEW.equals(message.getPath())) {
                    started.add(message.arg(1));
                } else {
                    assertEquals(List.of(PARAM_GATE, GATE_OFF), message.getArgs().subList(1, 3));
                    released.add(message.arg(0));
                }
            }
            assertEquals(2, started.size());
            assertEquals(2, released.size());
            assertTrue(released.containsAll(started));

            assertEquals(4, notified.size());
            assertEquals(SynthAction.NOTE_OFF, notified.get(2).getAction());
            assertEquals(SynthAction.NOTE_OFF, notified.get(3).getAction());
            assertEquals(124, notified.get(2).getNote() + notified.get(3).getNote());

            // el mapa quedó vacío: otro noteOff no envía nada
            command.execute(SynthEvent.organOff(60));
            command.execute(SynthEvent.organOff(64));
            assertNull(receiver.receive());
            assertEquals(4, notified.size());
        }
    }

    @Test
    void allNotesOffWithNoActiveNotesSendsNothing() throws Exception {
        List<SynthEvent> notified = new ArrayList<>();
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);
            command.addListener(notified::add);

            command.allNotesOff();

            assertNull(receiver.receive());
        }
        assertTrue(notified.isEmpty());
    }

    @Test
    void concurrentNoteOnAndNoteOffLeaveNoUntrackedNode() throws Exception {
        int threads = 4;
        int iterations = 50;
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            OrganCommand command = new OrganCommand(client);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        command.execute(SynthEvent.organOn(60));
                        command.execute(SynthEvent.organOff(60));
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            pool.shutdown();
            command.execute(SynthEvent.organOff(60));

            Map<Object, Integer> releasesPerNode = new HashMap<>();
            List<Object> startedNodes = new ArrayList<>();
            for (OscMessage message : receiver.receiveAll()) {
                if (OSC_S_NEW.equals(message.getPath())) {
                    startedNodes.add(message.arg(1));
                } else {
                    releasesPerNode.merge(message.arg(0), 1, Integer::sum);
                }
            }

            assertFalse(startedNodes.isEmpty());
            assertEquals(threads * iterations, startedNodes.size());
            for (Object node : startedNodes) {
                assertEquals(1, (int) releasesPerNode.getOrDefault(node, 0), "nodo " + node);
            }
            assertEquals(startedNodes.size(), releasesPerNode.size());
        }
    }
}
