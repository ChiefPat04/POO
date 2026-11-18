package powerband.editor.helpers;

import java.util.ArrayList;
import java.util.List;

import powerband.storage.IRecordingIndexer;

/**
 * Indexador de prueba: guarda el nombre y la posición de cada llamada a {@code add}.
 */
public class FakeIndexer implements IRecordingIndexer {

    private final List<String> names = new ArrayList<>();
    private final List<Long> positions = new ArrayList<>();

    @Override
    public void add(String pName, long pHeaderPosition) {
        names.add(pName);
        positions.add(pHeaderPosition);
    }

    public List<String> getNames() {
        return names;
    }

    public List<Long> getPositions() {
        return positions;
    }
}
