package powerband.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.SC_HOST;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.command.BassCommand;
import powerband.command.OrganCommand;
import powerband.command.PianoCommand;
import powerband.command.helpers.OscReceiver;
import powerband.editor.Recorder;
import powerband.model.IndexEntry;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;

/** Integración de ft-5 y ft-6: lo que guarda el grabador se encuentra en el índice. */
class RecordingIndexRecorderTest {

    @TempDir
    File tempDir;

    @Test
    void savedRecordingIsFoundInIndex() throws Exception {
        RecordingStorage storage = new RecordingStorage(
                new File(tempDir, "recordings.music"), new File(tempDir, "recordings.list"));
        RecordingIndex index = new RecordingIndex(new File(tempDir, "recordings.index"), tempDir);
        List<String> names = List.of("Zeta", "alfa", "Media");

        try (OscReceiver receiver = new OscReceiver();
                OscClient client = new OscClient(SC_HOST, receiver.getPort())) {
            PianoCommand piano = new PianoCommand(client);
            Recorder recorder = new Recorder(storage, index, piano,
                    new BassCommand(client), new OrganCommand(client));
            for (String name : names) {
                recorder.start();
                piano.execute(SynthEvent.piano(60, 500, 4));
                recorder.stop();
                recorder.save(name);
            }
        }

        assertEquals(names.size(), index.count());
        for (String name : names) {
            Optional<IndexEntry> found = index.find(name);
            assertTrue(found.isPresent(), "no se encontró " + name);
            assertEquals(name, storage.readHeader(found.get().getHeaderPosition()).getName());
        }
    }
}
