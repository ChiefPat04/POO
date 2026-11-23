package powerband.ui;

import static powerband.util.IConstants.BTN_LOAD;
import static powerband.util.IConstants.BTN_PLAY;
import static powerband.util.IConstants.BTN_RECORD;
import static powerband.util.IConstants.BTN_STOP_PLAYBACK;
import static powerband.util.IConstants.BTN_STOP_RECORDING;
import static powerband.util.IConstants.DIALOG_ERROR_TITLE;
import static powerband.util.IConstants.DIALOG_NAME_PROMPT;
import static powerband.util.IConstants.DIALOG_NAME_TITLE;
import static powerband.util.IConstants.ERR_INVALID_NAME;
import static powerband.util.IConstants.LIST_PANEL_WIDTH;
import static powerband.util.IConstants.NO_SELECTION;
import static powerband.util.IConstants.STATE_IDLE;
import static powerband.util.IConstants.STATE_NAMING;
import static powerband.util.IConstants.STATE_PLAYING;
import static powerband.util.IConstants.STATE_RECORDING;
import static powerband.util.IConstants.STATUS_LOADED;
import static powerband.util.IConstants.STATUS_PART_SEPARATOR;
import static powerband.util.IConstants.TITLE_MONITOR;
import static powerband.util.IConstants.TITLE_RECORDINGS;
import static powerband.util.IConstants.UI_GAP;
import static powerband.util.IConstants.WINDOW_HEIGHT;
import static powerband.util.IConstants.WINDOW_TITLE;
import static powerband.util.IConstants.WINDOW_WIDTH;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import powerband.command.ISynthCommand;
import powerband.editor.EditorController;
import powerband.editor.EditorState;
import powerband.editor.IEditorView;
import powerband.exception.DuplicateRecordingNameException;
import powerband.exception.PowerBandException;
import powerband.exception.StorageException;

/**
 * Ventana del editor. Solo muestra y llama a {@link EditorController}; las reglas
 * están en el controlador. Los avisos de {@link IEditorView} pueden llegar desde
 * otros hilos y se pasan al hilo de Swing.
 */
public class EditorWindow extends JFrame implements IEditorView {

    private static final long serialVersionUID = 1L;

    /** Una acción del controlador que puede fallar. */
    @FunctionalInterface
    private interface Action {
        void run() throws PowerBandException;
    }

    private final transient EditorController controller;
    private final transient CountDownLatch closedLatch;
    private final transient ISynthCommand[] commands;

    private final JButton recordButton = new JButton(BTN_RECORD);
    private final JButton stopRecordingButton = new JButton(BTN_STOP_RECORDING);
    private final JButton loadButton = new JButton(BTN_LOAD);
    private final JButton playButton = new JButton(BTN_PLAY);
    private final JButton stopPlaybackButton = new JButton(BTN_STOP_PLAYBACK);
    private final RecordingListModel listModel;
    private final JList<String> list;
    private final MonitorPanel monitor = new MonitorPanel();
    private final JLabel statusBar = new JLabel();

    private EditorState state;
    private boolean closed;

    public EditorWindow(EditorController pController, CountDownLatch pClosed,
            ISynthCommand... pCommands) {
        super(WINDOW_TITLE);
        controller = pController;
        closedLatch = pClosed;
        commands = pCommands;
        listModel = new RecordingListModel(pController);
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e -> applyState());

        buildLayout();
        wireButtons();
        for (ISynthCommand command : commands) {
            command.addListener(monitor);
        }

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent pEvent) {
                closeWindow();
            }
        });
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);

        state = controller.getState();
        controller.setView(this);
        applyState();
    }

    // ------------------------------------------------------------ IEditorView

    @Override
    public void stateChanged(EditorState pState) {
        SwingUtilities.invokeLater(() -> {
            state = pState;
            applyState();
        });
    }

    @Override
    public void recordingsChanged() {
        SwingUtilities.invokeLater(listModel::refresh);
    }

    @Override
    public void showError(String pMessage) {
        SwingUtilities.invokeLater(() -> showErrorNow(pMessage));
    }

    // ------------------------------------------------------------ armado

    private void buildLayout() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, UI_GAP, UI_GAP));
        buttons.add(recordButton);
        buttons.add(stopRecordingButton);
        buttons.add(loadButton);
        buttons.add(playButton);
        buttons.add(stopPlaybackButton);

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createTitledBorder(TITLE_RECORDINGS));
        listScroll.setPreferredSize(new Dimension(LIST_PANEL_WIDTH, WINDOW_HEIGHT));
        monitor.setBorder(BorderFactory.createTitledBorder(TITLE_MONITOR));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, monitor);

        statusBar.setBorder(BorderFactory.createEmptyBorder(UI_GAP, UI_GAP, UI_GAP, UI_GAP));

        setLayout(new BorderLayout());
        add(buttons, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void wireButtons() {
        recordButton.addActionListener(e -> run(controller::startRecording));
        stopRecordingButton.addActionListener(e -> stopRecordingAndAskName());
        loadButton.addActionListener(e -> run(this::loadSelected));
        playButton.addActionListener(e -> run(controller::play));
        stopPlaybackButton.addActionListener(e -> controller.stopPlayback());
    }

    // ------------------------------------------------------------ acciones

    /** Ejecuta la acción; cualquier error se muestra, nunca cierra la ventana. */
    private void run(Action pAction) {
        try {
            pAction.run();
        } catch (PowerBandException | IllegalStateException e) {
            showErrorNow(e.getMessage());
        }
        applyState();
    }

    private void loadSelected() throws PowerBandException {
        int selected = list.getSelectedIndex();
        if (selected != NO_SELECTION) {
            controller.load(listModel.nameAt(selected));
        }
    }

    /** Detiene la grabación y pide el nombre hasta que sea válido y único, o se cancele. */
    private void stopRecordingAndAskName() {
        run(controller::stopRecording);
        while (controller.getState() == EditorState.NAMING) {
            String name = JOptionPane.showInputDialog(this, DIALOG_NAME_PROMPT,
                    DIALOG_NAME_TITLE, JOptionPane.QUESTION_MESSAGE);
            if (name == null) {
                run(controller::discardRecording);
                return;
            }
            if (!EditorController.isValidName(name)) {
                showErrorNow(ERR_INVALID_NAME + name);
                continue;
            }
            try {
                controller.saveRecording(name);
            } catch (DuplicateRecordingNameException | StorageException e) {
                showErrorNow(e.getMessage()); // el ciclo sigue si quedó esperando nombre
            }
        }
        applyState();
    }

    private void closeWindow() {
        if (closed) {
            return;
        }
        closed = true;
        controller.shutdown();
        for (ISynthCommand command : commands) {
            command.removeListener(monitor);
        }
        dispose();
        closedLatch.countDown();
    }

    // ------------------------------------------------------------ vista

    /** Activa los botones según el estado, la selección y la grabación cargada. */
    private void applyState() {
        boolean idle = state == EditorState.IDLE;
        boolean loaded = controller.getLoadedName() != null;
        recordButton.setEnabled(idle);
        stopRecordingButton.setEnabled(state == EditorState.RECORDING);
        loadButton.setEnabled(idle && list.getSelectedIndex() != NO_SELECTION);
        playButton.setEnabled(idle && loaded);
        stopPlaybackButton.setEnabled(state == EditorState.PLAYING);

        String status = controller.getInstrumentStatus() + STATUS_PART_SEPARATOR + stateText();
        if (loaded) {
            status += STATUS_PART_SEPARATOR + STATUS_LOADED + controller.getLoadedName();
        }
        statusBar.setText(status);
    }

    private String stateText() {
        switch (state) {
            case RECORDING:
                return STATE_RECORDING;
            case NAMING:
                return STATE_NAMING;
            case PLAYING:
                return STATE_PLAYING;
            default:
                return STATE_IDLE;
        }
    }

    private void showErrorNow(String pMessage) {
        JOptionPane.showMessageDialog(this, pMessage, DIALOG_ERROR_TITLE,
                JOptionPane.ERROR_MESSAGE);
    }
}
