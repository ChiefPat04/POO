package powerband.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static powerband.util.IConstants.GATE_OFF;
import static powerband.util.IConstants.LABEL_GAMEPAD;
import static powerband.util.IConstants.LABEL_MICROPHONE;
import static powerband.util.IConstants.LABEL_WEBCAM;
import static powerband.util.IConstants.LIST_DATE_PATTERN;
import static powerband.util.IConstants.MONITOR_DUR_SUFFIX;
import static powerband.util.IConstants.MONITOR_NO_VALUE;
import static powerband.util.IConstants.OSC_N_SET;
import static powerband.util.IConstants.PARAM_GATE;
import static powerband.util.IConstants.PLAYER_STOP_TIMEOUT_MS;
import static powerband.util.IConstants.SC_HOST;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import powerband.command.BassCommand;
import powerband.command.OrganCommand;
import powerband.command.PianoCommand;
import powerband.command.helpers.OscMessage;
import powerband.command.helpers.OscReceiver;
import powerband.editor.helpers.FakeEditorView;
import powerband.editor.helpers.RecordingFiles;
import powerband.editor.helpers.StubInstrument;
import powerband.exception.DuplicateRecordingNameException;
import powerband.exception.RecordingNotFoundException;
import powerband.instrument.helpers.FakeInstrument;
import powerband.model.ListEntry;
import powerband.model.SynthEvent;
import powerband.sc.OscClient;
import powerband.storage.RecordingStorage;

class EditorControllerTest {

    private static final long WAIT_TIMEOUT_MS = 3000L;
    private static final long WAIT_STEP_MS = 5L;
    private static final long LONG_EVENT_MS = 5000L;

    @TempDir
    File tempDir;

    private OscReceiver receiver;
    private OscClient client;
    private PianoCommand piano;
    private BassCommand bass;
    private OrganCommand organ;
    private RecordingFiles files;
    private RecordingStorage storage;
    private Player player;
    private FakeEditorView view;
    private EditorController controller;

    @BeforeEach
    void setUp() throws Exception {
        receiver = new OscReceiver();
        client = new OscClient(SC_HOST, receiver.getPort());
        piano = new PianoCommand(client);
        bass = new BassCommand(client);
        organ = new OrganCommand(client);
        files = new RecordingFiles(tempDir);
        storage = files.getStorage();
        Recorder recorder = new Recorder(storage, files.getIndex(), piano, bass, organ);
        player = new Player(storage, piano, bass, organ);
        RecordingLibrary library = new RecordingLibrary(storage, files.getIndex());
        controller = new EditorController(recorder, player, library, organ);
        view = new FakeEditorView();
        controller.setView(view);
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
        client.close();
        receiver.close();
    }

    // ------------------------------------------------------------ reglas de estado

    @Test
    void startsIdle() {
        assertEquals(EditorState.IDLE, controller.getState());
        assertNull(controller.getLoadedName());
    }

    @Test
    void cannotRecordWhilePlaying() throws Exception {
        files.save("larga", SynthEvent.piano(60, 500, 4).withTimeMs(LONG_EVENT_MS));
        controller.load("larga");
        controller.play();
        long lengthBefore = storage.musicLength();

        assertThrows(IllegalStateException.class, () -> controller.startRecording());

        assertEquals(EditorState.PLAYING, controller.getState());
        assertEquals(lengthBefore, storage.musicLength());
    }

    @Test
    void cannotPlayWhileRecording() throws Exception {
        files.save("corta", SynthEvent.piano(60, 500, 4).withTimeMs(0L));
        controller.load("corta");
        controller.startRecording();

        assertThrows(IllegalStateException.class, () -> controller.play());

        assertEquals(EditorState.RECORDING, controller.getState());
        assertNull(receiver.receive());
    }

    @Test
    void cannotPlayWhileNaming() throws Exception {
        files.save("corta", SynthEvent.piano(60, 500, 4).withTimeMs(0L));
        controller.load("corta");
        controller.startRecording();
        controller.stopRecording();

        assertThrows(IllegalStateException.class, () -> controller.play());
        assertThrows(IllegalStateException.class, () -> controller.load("corta"));

        assertEquals(EditorState.NAMING, controller.getState());
    }

    @Test
    void cannotRecordTwice() throws Exception {
        controller.startRecording();

        assertThrows(IllegalStateException.class, () -> controller.startRecording());
    }

    // ------------------------------------------------------------ grabación

    @Test
    void recordDuplicateNameThenOtherNameSaves() throws Exception {
        files.save("Cancion1", SynthEvent.piano(60, 500, 4).withTimeMs(0L));
        FakeInstrument instrument = new FakeInstrument(bass);

        controller.startRecording();
        instrument.playEvent(SynthEvent.bass(36, 450));
        controller.stopRecording();
        assertEquals(EditorState.NAMING, controller.getState());

        assertThrows(DuplicateRecordingNameException.class,
                () -> controller.saveRecording("Cancion1"));
        assertEquals(EditorState.NAMING, controller.getState());
        assertEquals(0, view.getRecordingsChanged());

        controller.saveRecording("Cancion2");

        assertEquals(EditorState.IDLE, controller.getState());
        assertEquals(2L, controller.recordingCount());
        assertEquals("Cancion2", controller.recordingAt(1).getName());
        assertEquals(1, view.getRecordingsChanged());
    }

    @Test
    void invalidNameKeepsNaming() throws Exception {
        controller.startRecording();
        controller.stopRecording();

        assertThrows(IllegalArgumentException.class, () -> controller.saveRecording("canción"));

        assertEquals(EditorState.NAMING, controller.getState());
        assertFalse(EditorController.isValidName("canción"));
        assertTrue(EditorController.isValidName("Cancion1"));
    }

    @Test
    void cancelDiscardsRecording() throws Exception {
        long lengthBefore = storage.musicLength();
        FakeInstrument instrument = new FakeInstrument(piano);

        controller.startRecording();
        instrument.playEvent(SynthEvent.piano(60, 500, 4));
        controller.stopRecording();
        controller.discardRecording();

        assertEquals(EditorState.IDLE, controller.getState());
        assertEquals(lengthBefore, storage.musicLength());
        assertEquals(0L, controller.recordingCount());
    }

    @Test
    void stateChangesAreReportedToView() throws Exception {
        controller.startRecording();
        controller.stopRecording();
        controller.saveRecording("Cancion1");

        assertEquals(List.of(EditorState.RECORDING, EditorState.NAMING, EditorState.IDLE),
                view.getStates());
    }

    // ------------------------------------------------------------ carga y reproducción

    @Test
    void loadThenPlaySavedRecordingReturnsToIdle() throws Exception {
        files.save("corta", SynthEvent.piano(60, 500, 4).withTimeMs(0L),
                SynthEvent.bass(36, 450).withTimeMs(20L));

        controller.load("corta");
        controller.play();

        assertNotNull(receiver.receive());
        assertNotNull(receiver.receive());
        awaitTrue(() -> controller.getState() == EditorState.IDLE);
        assertEquals(List.of(EditorState.PLAYING, EditorState.IDLE), view.getStates());
        assertEquals("corta", controller.getLoadedName());
    }

    @Test
    void playWithoutLoadThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> controller.play());

        assertEquals(EditorState.IDLE, controller.getState());
    }

    @Test
    void stopPlaybackReturnsToIdleQuickly() throws Exception {
        files.save("larga", SynthEvent.piano(60, 500, 4).withTimeMs(LONG_EVENT_MS));
        controller.load("larga");
        controller.play();

        long before = System.currentTimeMillis();
        controller.stopPlayback();
        long elapsed = System.currentTimeMillis() - before;

        assertTrue(elapsed < PLAYER_STOP_TIMEOUT_MS, "tardó " + elapsed + " ms");
        assertEquals(EditorState.IDLE, controller.getState());
        assertNull(receiver.receive());
    }

    @Test
    void loadUnknownNameThrowsAndKeepsPreviousLoad() throws Exception {
        files.save("corta", SynthEvent.piano(60, 500, 4).withTimeMs(0L));
        controller.load("corta");

        assertThrows(RecordingNotFoundException.class, () -> controller.load("otra"));

        assertEquals(EditorState.IDLE, controller.getState());
        assertEquals("corta", controller.getLoadedName());
    }

    // ------------------------------------------------------------ instrumentos y cierre

    @Test
    void failingInstrumentDoesNotStopOthers() {
        StubInstrument gamepad = new StubInstrument(bass);
        StubInstrument microphone = new StubInstrument(organ);
        StubInstrument webcam = new StubInstrument(piano);
        microphone.setFailOnStart(true);
        controller.addInstrument(LABEL_GAMEPAD, gamepad);
        controller.addInstrument(LABEL_MICROPHONE, microphone);
        controller.addInstrument(LABEL_WEBCAM, webcam);

        controller.startInstruments();

        assertTrue(gamepad.isStarted());
        assertTrue(webcam.isStarted());
        String status = controller.getInstrumentStatus();
        assertTrue(status.contains(LABEL_GAMEPAD), status);
        assertTrue(status.contains(LABEL_MICROPHONE), status);
        assertTrue(status.contains(LABEL_WEBCAM), status);
        assertTrue(status.contains(StubInstrument.START_FAILURE), status);
    }

    @Test
    void shutdownWhileRecordingIsOrdered() throws Exception {
        StubInstrument working = new StubInstrument(bass);
        StubInstrument broken = new StubInstrument(piano);
        broken.setFailOnStart(true);
        controller.addInstrument(LABEL_GAMEPAD, working);
        controller.addInstrument(LABEL_WEBCAM, broken);
        controller.startInstruments();
        long lengthBefore = storage.musicLength();

        controller.startRecording();
        new FakeInstrument(organ).playEvent(SynthEvent.organOn(60));
        OscMessage noteOn = receiver.receive();

        controller.shutdown();

        assertEquals(lengthBefore, storage.musicLength());
        assertFalse(player.isPlaying());
        assertTrue(working.isStopped());
        assertFalse(broken.isStopped());
        OscMessage noteOff = receiver.receive();
        assertEquals(OSC_N_SET, noteOff.getPath());
        assertEquals(List.of(noteOn.arg(1), PARAM_GATE, GATE_OFF), noteOff.getArgs());
        assertTrue(view.getErrors().isEmpty(), view.getErrors().toString());
    }

    @Test
    void shutdownWhilePlayingStopsPlayer() throws Exception {
        files.save("larga", SynthEvent.piano(60, 500, 4).withTimeMs(LONG_EVENT_MS));
        controller.load("larga");
        controller.play();

        controller.shutdown();

        assertFalse(player.isPlaying());
        assertEquals(EditorState.IDLE, controller.getState());
    }

    @Test
    void shutdownTwiceDoesNothingTheSecondTime() {
        StubInstrument instrument = new StubInstrument(bass);
        instrument.setFailOnStop(true);
        controller.addInstrument(LABEL_GAMEPAD, instrument);
        controller.startInstruments();

        controller.shutdown();
        controller.shutdown();

        assertEquals(1, view.getErrors().size());
    }

    @Test
    void shutdownReportsFailingStepAndContinues() {
        StubInstrument failing = new StubInstrument(bass);
        StubInstrument other = new StubInstrument(piano);
        failing.setFailOnStop(true);
        controller.addInstrument(LABEL_GAMEPAD, failing);
        controller.addInstrument(LABEL_WEBCAM, other);
        controller.startInstruments();

        controller.shutdown();

        assertEquals(1, view.getErrors().size());
        assertTrue(view.getErrors().get(0).contains(StubInstrument.STOP_FAILURE));
        assertTrue(other.isStopped());
    }

    // ------------------------------------------------------------ formatos

    @Test
    void formatEventShowsTimeSynthActionNoteAndDuration() {
        String piano = EditorController.formatEvent(
                SynthEvent.piano(60, 500, 4).withTimeMs(System.currentTimeMillis()));
        String organOn = EditorController.formatEvent(
                SynthEvent.organOn(69).withTimeMs(System.currentTimeMillis()));

        assertTrue(piano.contains("PIANO"), piano);
        assertTrue(piano.contains("TRIGGER"), piano);
        assertTrue(piano.contains("60"), piano);
        assertTrue(piano.contains("500" + MONITOR_DUR_SUFFIX), piano);
        assertTrue(organOn.contains("NOTE_ON"), organOn);
        assertTrue(organOn.endsWith(MONITOR_NO_VALUE), organOn);
    }

    @Test
    void formatListEntryShowsNameAndDate() {
        long createdAt = 1_700_000_000_000L;
        String expectedDate = DateTimeFormatter.ofPattern(LIST_DATE_PATTERN)
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(createdAt));

        String line = EditorController.formatListEntry(new ListEntry("Cancion1", createdAt));

        assertTrue(line.startsWith("Cancion1"), line);
        assertTrue(line.endsWith(expectedDate), line);
    }

    // ------------------------------------------------------------ auxiliares

    private static void awaitTrue(BooleanSupplier pCondition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MS;
        while (!pCondition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(WAIT_STEP_MS);
        }
        assertTrue(pCondition.getAsBoolean(), "no se cumplió a tiempo");
    }
}
