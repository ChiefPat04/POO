package mutantes.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import mutantes.model.poderes.TipoEfecto;

public class DialogoAyudaPoderes extends JDialog {

    public DialogoAyudaPoderes(java.awt.Frame padre) {
        super(padre, "Guia de Poderes", true);
        setSize(480, 500);
        setLocationRelativeTo(padre);
        setLayout(new BorderLayout());

        JPanel listaPoderes = new JPanel();
        listaPoderes.setLayout(new BoxLayout(listaPoderes, BoxLayout.Y_AXIS));
        listaPoderes.setBackground(Color.WHITE);
        listaPoderes.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (TipoEfecto tipo : TipoEfecto.values()) {
            listaPoderes.add(crearFilaPoder(tipo));
            listaPoderes.add(javax.swing.Box.createVerticalStrut(8));
        }

        add(new JScrollPane(listaPoderes), BorderLayout.CENTER);
    }

    private JPanel crearFilaPoder(TipoEfecto tipo) {
        JPanel fila = new JPanel(new BorderLayout(10, 0));
        fila.setBackground(Color.WHITE);
        fila.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        fila.add(new IconoPoderSwatch(tipo), BorderLayout.WEST);

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setBackground(Color.WHITE);

        JLabel nombre = new JLabel(EfectosVisuales.nombreLegibleParaEfecto(tipo));
        nombre.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel descripcion = new JLabel("<html><body style='width: 300px'>"
                + EfectosVisuales.descripcionParaEfecto(tipo) + "</body></html>");
        descripcion.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descripcion.setForeground(Color.DARK_GRAY);

        textos.add(nombre);
        textos.add(descripcion);
        fila.add(textos, BorderLayout.CENTER);

        return fila;
    }

    private static class IconoPoderSwatch extends JPanel {

        private final TipoEfecto tipo;

        IconoPoderSwatch(TipoEfecto tipo) {
            this.tipo = tipo;
            setPreferredSize(new Dimension(36, 36));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(EfectosVisuales.colorParaEfecto(tipo));
            g.fillOval(2, 2, 30, 30);
            g.setColor(Color.BLACK);
            g.drawString(EfectosVisuales.simboloParaEfecto(tipo), 14, 22);
        }
    }
}