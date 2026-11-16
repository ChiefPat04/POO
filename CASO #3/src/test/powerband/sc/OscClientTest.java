package powerband.sc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static powerband.util.IConstants.FIRST_NODE_ID;
import static powerband.util.IConstants.OSC_S_NEW;
import static powerband.util.IConstants.SC_HOST;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import powerband.command.helpers.OscMessage;
import powerband.command.helpers.OscReceiver;
import powerband.exception.SynthConnectionException;

class OscClientTest {

    @Test
    void encodesStringWithNullTerminatorAndPaddingToFourBytes() {
        // "/s_new" (6 bytes) + null + relleno = 8; luego el type tag "," ocupa 4
        byte[] sNew = OscClient.encode(OSC_S_NEW);
        assertEquals(12, sNew.length);
        assertArrayEquals(bytes("/s_new\0\0"), Arrays.copyOfRange(sNew, 0, 8));

        // un string de 4 bytes necesita 4 bytes más para el null: ocupa 8
        byte[] four = OscClient.encode("abcd");
        assertEquals(12, four.length);
        assertArrayEquals(bytes("abcd\0\0\0\0"), Arrays.copyOfRange(four, 0, 8));
    }

    @Test
    void encodesTypeTagsForIntFloatAndString() {
        byte[] data = OscClient.encode("/a", 1, 2f, "x");

        // "/a" ocupa 4; ",ifs" ocupa 8
        assertArrayEquals(bytes(",ifs\0\0\0\0"), Arrays.copyOfRange(data, 4, 12));
    }

    @Test
    void encodesIntsBigEndianAndFloatsAsIntBits() {
        byte[] intData = OscClient.encode("/a", 0x01020304);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, Arrays.copyOfRange(intData, 8, 12));

        byte[] floatData = OscClient.encode("/a", 1.5f);
        int bits = ByteBuffer.wrap(floatData, 8, 4).getInt();
        assertEquals(Float.floatToIntBits(1.5f), bits);
    }

    @Test
    void rejectsUnsupportedArgumentType() {
        assertThrows(IllegalArgumentException.class, () -> OscClient.encode("/a", 1.0d));
        assertThrows(IllegalArgumentException.class, () -> OscClient.encode("/a", 1L));
    }

    @Test
    void sendsSNewWithFixedArgsBeforeControls() throws Exception {
        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            client.sNew("synth", 10001, "note", 60);

            OscMessage message = receiver.receive();
            assertEquals(OSC_S_NEW, message.getPath());
            assertEquals(List.of("synth", 10001, 0, 1, "note", 60), message.getArgs());
        }
    }

    @Test
    void nodeIdsStartAtFirstNodeIdAndAreUniqueAcrossThreads() throws Exception {
        int threads = 8;
        int idsPerThread = 500;
        Set<Integer> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());

        try (OscClient client = new OscClient(SC_HOST, 57110)) {
            assertEquals(FIRST_NODE_ID, client.nextNodeId());
            ids.add(FIRST_NODE_ID);

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    awaitQuietly(start);
                    for (int i = 0; i < idsPerThread; i++) {
                        ids.add(client.nextNodeId());
                    }
                });
            }
            start.countDown();
            pool.shutdown();
            assertEquals(true, pool.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(threads * idsPerThread + 1, ids.size());
        assertEquals(FIRST_NODE_ID, (int) Collections.min(ids));
    }

    @Test
    void throwsSynthConnectionExceptionForInvalidPort() {
        assertThrows(SynthConnectionException.class, () -> new OscClient(SC_HOST, -1));
        assertThrows(SynthConnectionException.class, () -> new OscClient(SC_HOST, 70000));
    }

    @Test
    void throwsSynthConnectionExceptionForUnknownHost() {
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            assertThrows(SynthConnectionException.class,
                    () -> new OscClient("power-band-host-inexistente.invalid", 57110));
        });
    }

    private static byte[] bytes(String pText) {
        return pText.getBytes(StandardCharsets.US_ASCII);
    }

    private static void awaitQuietly(CountDownLatch pLatch) {
        try {
            pLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
