package powerband.model;

/**
 * Encabezado de una grabación en recordings.music. Inmutable.
 * La posición de inicio es la del primer comando; la de fin, la siguiente al último.
 */
public final class RecordingHeader {

    private final String name;
    private final long startPosition;
    private final long endPosition;

    public RecordingHeader(String pName, long pStartPosition, long pEndPosition) {
        name = pName;
        startPosition = pStartPosition;
        endPosition = pEndPosition;
    }

    public String getName() {
        return name;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public long getEndPosition() {
        return endPosition;
    }
}
