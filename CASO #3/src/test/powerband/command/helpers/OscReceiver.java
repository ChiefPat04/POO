package powerband.command.helpers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Sustituto de scsynth para las pruebas: escucha en un puerto UDP efímero de
 * localhost y decodifica los mensajes OSC que recibe.
 */
public class OscReceiver implements AutoCloseable {

    private static final int BUFFER_SIZE = 2048;
    private static final int SOCKET_BUFFER_SIZE = 4 * 1024 * 1024;
    private static final int TIMEOUT_MS = 300;
    private static final int ALIGNMENT = 4;

    private final DatagramSocket socket;

    public OscReceiver() throws IOException {
        socket = new DatagramSocket(0, InetAddress.getLoopbackAddress());
        socket.setSoTimeout(TIMEOUT_MS);
        socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
    }

    public int getPort() {
        return socket.getLocalPort();
    }

    /** Recibe un mensaje, o {@code null} si no llegó nada antes del timeout. */
    public OscMessage receive() throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            return null; // no llegó nada: es el resultado esperado en las pruebas de "no envía"
        }
        return decode(ByteBuffer.wrap(packet.getData(), 0, packet.getLength()));
    }

    /** Recibe mensajes hasta que pase un timeout sin que llegue ninguno. */
    public List<OscMessage> receiveAll() throws IOException {
        List<OscMessage> messages = new ArrayList<>();
        OscMessage message = receive();
        while (message != null) {
            messages.add(message);
            message = receive();
        }
        return messages;
    }

    @Override
    public void close() {
        socket.close();
    }

    private static OscMessage decode(ByteBuffer pBuffer) {
        String path = readString(pBuffer);
        String tags = readString(pBuffer);
        List<Object> args = new ArrayList<>();
        for (int i = 1; i < tags.length(); i++) {
            switch (tags.charAt(i)) {
                case 'i' -> args.add(pBuffer.getInt());
                case 'f' -> args.add(pBuffer.getFloat());
                case 's' -> args.add(readString(pBuffer));
                default -> throw new IllegalStateException("Tag OSC desconocido: " + tags.charAt(i));
            }
        }
        return new OscMessage(path, args);
    }

    private static String readString(ByteBuffer pBuffer) {
        int start = pBuffer.position();
        int end = start;
        while (pBuffer.get(end) != 0) {
            end++;
        }
        byte[] bytes = new byte[end - start];
        pBuffer.get(bytes);
        int padded = ((bytes.length / ALIGNMENT) + 1) * ALIGNMENT;
        pBuffer.position(start + padded);
        return new String(bytes, StandardCharsets.US_ASCII);
    }
}
