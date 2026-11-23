package powerband.ui;

import static powerband.util.IConstants.FIRST_INDEX;
import static powerband.util.IConstants.MONITOR_FONT_SIZE;
import static powerband.util.IConstants.MONITOR_MAX_LINES;
import static powerband.util.IConstants.MONO_FONT;
import static powerband.util.IConstants.NEWLINE;

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import powerband.command.ISynthEventListener;
import powerband.editor.EditorController;
import powerband.model.SynthEvent;

/**
 * Monitor en vivo de cada comando enviado a SuperCollider. Los eventos llegan
 * desde los hilos de los instrumentos y del reproductor; el texto se toca solo
 * en el hilo de Swing. Guarda como máximo {@code MONITOR_MAX_LINES} líneas.
 */
public class MonitorPanel extends JScrollPane implements ISynthEventListener {

    private static final long serialVersionUID = 1L;

    private final JTextArea text = new JTextArea();

    public MonitorPanel() {
        text.setEditable(false);
        text.setFont(new Font(MONO_FONT, Font.PLAIN, MONITOR_FONT_SIZE));
        setViewportView(text);
    }

    @Override
    public void onSynthEvent(SynthEvent pEvent) {
        String line = EditorController.formatEvent(pEvent) + NEWLINE;
        SwingUtilities.invokeLater(() -> append(line));
    }

    private void append(String pLine) {
        text.append(pLine);
        int extra = text.getLineCount() - 1 - MONITOR_MAX_LINES; // la última línea está vacía
        if (extra > FIRST_INDEX) {
            try {
                text.replaceRange(null, FIRST_INDEX, text.getLineEndOffset(extra - 1));
            } catch (BadLocationException e) {
                text.setText(pLine); // no debería pasar: se reinicia el monitor
            }
        }
        text.setCaretPosition(text.getDocument().getLength());
    }
}
