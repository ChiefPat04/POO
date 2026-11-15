package powerband.sc;

import static powerband.util.IConstants.ADD_TO_HEAD;
import static powerband.util.IConstants.BYTE_SHIFT_16;
import static powerband.util.IConstants.BYTE_SHIFT_24;
import static powerband.util.IConstants.BYTE_SHIFT_8;
import static powerband.util.IConstants.DEFAULT_GROUP;
import static powerband.util.IConstants.ERR_INVALID_PORT;
import static powerband.util.IConstants.ERR_SEND;
import static powerband.util.IConstants.ERR_SOCKET_CREATE;
import static powerband.util.IConstants.ERR_UNKNOWN_HOST;
import static powerband.util.IConstants.ERR_UNSUPPORTED_OSC_TYPE;
import static powerband.util.IConstants.FIRST_NODE_ID;
import static powerband.util.IConstants.OSC_ALIGNMENT;
import static powerband.util.IConstants.OSC_NULL_BYTE;
import static powerband.util.IConstants.OSC_N_SET;
import static powerband.util.IConstants.OSC_S_NEW;
import static powerband.util.IConstants.OSC_TAG_FLOAT;
import static powerband.util.IConstants.OSC_TAG_INT;
import static powerband.util.IConstants.OSC_TAG_STRING;
import static powerband.util.IConstants.OSC_TYPE_TAG_PREFIX;
import static powerband.util.IConstants.PORT_MAX;
import static powerband.util.IConstants.PORT_MIN;
import static powerband.util.IConstants.S_NEW_FIXED_ARGS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import powerband.exception.SynthConnectionException;

/**
 * Cliente OSC mínimo hacia scsynth por UDP. Codifica y envía {@code /s_new} y
 * {@code /n_set}, siguiendo el algoritmo de {@code recursos/supercollider/BandClient.java}.
 *
 * <p>Es seguro usarlo desde varios hilos: los {@code nodeId} salen de un
 * {@link AtomicInteger} y {@link DatagramSocket#send} es thread-safe.</p>
 */
public class OscClient implements AutoCloseable {

    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;
    private final AtomicInteger nodeIdCounter = new AtomicInteger(FIRST_NODE_ID);

    public OscClient(String pHost, int pPort) throws SynthConnectionException {
        if (pPort < PORT_MIN || pPort > PORT_MAX) {
            throw new SynthConnectionException(ERR_INVALID_PORT + pPort);
        }
        try {
            address = InetAddress.getByName(pHost);
        } catch (UnknownHostException e) {
            throw new SynthConnectionException(ERR_UNKNOWN_HOST + pHost, e);
        }
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new SynthConnectionException(ERR_SOCKET_CREATE, e);
        }
        port = pPort;
    }

    /** Devuelve un nodeId nuevo; nunca se repite, aun con varios hilos. */
    public int nextNodeId() {
        return nodeIdCounter.getAndIncrement();
    }

    /** Envía {@code /s_new defName nodeId addAction target [nombre valor ...]}. */
    public void sNew(String pDefName, int pNodeId, Object... pControls) throws SynthConnectionException {
        List<Object> args = new ArrayList<>(S_NEW_FIXED_ARGS + pControls.length);
        args.add(pDefName);
        args.add(pNodeId);
        args.add(ADD_TO_HEAD);
        args.add(DEFAULT_GROUP);
        args.addAll(Arrays.asList(pControls));
        send(OSC_S_NEW, args.toArray());
    }

    /** Envía {@code /n_set nodeId [nombre valor ...]}. */
    public void nSet(int pNodeId, Object... pControls) throws SynthConnectionException {
        List<Object> args = new ArrayList<>();
        args.add(pNodeId);
        args.addAll(Arrays.asList(pControls));
        send(OSC_N_SET, args.toArray());
    }

    /**
     * Codifica un mensaje OSC. Soporta {@link Integer}, {@link Float} y {@link String}.
     *
     * @throws IllegalArgumentException si algún argumento es de otro tipo
     */
    public static byte[] encode(String pPath, Object... pArgs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, pPath);

        StringBuilder tags = new StringBuilder().append(OSC_TYPE_TAG_PREFIX);
        for (Object arg : pArgs) {
            tags.append(typeTagOf(arg));
        }
        writeString(out, tags.toString());

        for (Object arg : pArgs) {
            if (arg instanceof Integer) {
                writeInt(out, (Integer) arg);
            } else if (arg instanceof Float) {
                writeInt(out, Float.floatToIntBits((Float) arg));
            } else {
                writeString(out, (String) arg);
            }
        }
        return out.toByteArray();
    }

    /** Solo cierra el socket. Apagar notas del órgano pendientes queda para ft-5. */
    @Override
    public void close() {
        socket.close();
    }

    // ------------------------------------------------------------ auxiliares

    private void send(String pPath, Object... pArgs) throws SynthConnectionException {
        byte[] data = encode(pPath, pArgs);
        try {
            socket.send(new DatagramPacket(data, data.length, address, port));
        } catch (IOException e) {
            throw new SynthConnectionException(ERR_SEND + pPath, e);
        }
    }

    private static char typeTagOf(Object pArg) {
        if (pArg instanceof Integer) {
            return OSC_TAG_INT;
        }
        if (pArg instanceof Float) {
            return OSC_TAG_FLOAT;
        }
        if (pArg instanceof String) {
            return OSC_TAG_STRING;
        }
        throw new IllegalArgumentException(ERR_UNSUPPORTED_OSC_TYPE
                + (pArg == null ? null : pArg.getClass().getName()));
    }

    /** Entero de 32 bits en big-endian. */
    private static void writeInt(ByteArrayOutputStream pOut, int pValue) {
        pOut.write(pValue >>> BYTE_SHIFT_24);
        pOut.write(pValue >>> BYTE_SHIFT_16);
        pOut.write(pValue >>> BYTE_SHIFT_8);
        pOut.write(pValue);
    }

    /** String OSC: bytes ASCII + terminador null, rellenado a múltiplo de 4. */
    private static void writeString(ByteArrayOutputStream pOut, String pValue) {
        byte[] bytes = pValue.getBytes(StandardCharsets.US_ASCII);
        pOut.writeBytes(bytes);
        int padding = OSC_ALIGNMENT - (bytes.length % OSC_ALIGNMENT); // siempre agrega al menos el null
        for (int i = 0; i < padding; i++) {
            pOut.write(OSC_NULL_BYTE);
        }
    }
}
