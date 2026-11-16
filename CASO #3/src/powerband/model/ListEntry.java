package powerband.model;

/**
 * Registro de recordings.list: nombre de la grabación y fecha de creación en ms. Inmutable.
 */
public final class ListEntry {

    private final String name;
    private final long createdAtMs;

    public ListEntry(String pName, long pCreatedAtMs) {
        name = pName;
        createdAtMs = pCreatedAtMs;
    }

    public String getName() {
        return name;
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }
}
