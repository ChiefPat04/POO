package powerband.command.helpers;

import java.util.List;

/**
 * Mensaje OSC decodificado por {@link OscReceiver}: path y argumentos
 * ({@link Integer}, {@link Float} o {@link String}).
 */
public final class OscMessage {

    private final String path;
    private final List<Object> args;

    public OscMessage(String pPath, List<Object> pArgs) {
        path = pPath;
        args = List.copyOf(pArgs);
    }

    public String getPath() {
        return path;
    }

    public List<Object> getArgs() {
        return args;
    }

    public Object arg(int pIndex) {
        return args.get(pIndex);
    }

    @Override
    public String toString() {
        return path + " " + args;
    }
}
