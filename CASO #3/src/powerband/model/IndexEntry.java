package powerband.model;

/**
 * Registro de recordings.index: nombre de la grabación y posición de su
 * encabezado en recordings.music. Inmutable.
 */
public final class IndexEntry {

    private final String name;
    private final long headerPosition;

    public IndexEntry(String pName, long pHeaderPosition) {
        name = pName;
        headerPosition = pHeaderPosition;
    }

    public String getName() {
        return name;
    }

    public long getHeaderPosition() {
        return headerPosition;
    }
}
