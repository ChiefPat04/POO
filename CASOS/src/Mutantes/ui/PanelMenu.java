package mutantes.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import mutantes.constants.ConstantesJuego;

public class PanelMenu extends JPanel {

    private static final Color VERDE_TOXICO = new Color(120, 220, 60);
    private static final Color VERDE_TOXICO_OSCURO = new Color(70, 140, 30);
    private static final Color FONDO_PANEL = new Color(15, 10, 12, 210);

    private BufferedImage imgFondo;

    public PanelMenu(IntConsumer alIniciar) {
        setLayout(new GridBagLayout());
        setBackground(new Color(20, 10, 15));
        cargarImagenFondo();

        JPanel panelControles = crearPanelControles(alIniciar);

        GridBagConstraints gbcExterno = new GridBagConstraints();
        gbcExterno.gridx = 0;
        gbcExterno.gridy = 0;
        gbcExterno.weighty = 1.0;
        gbcExterno.anchor = GridBagConstraints.SOUTH;
        gbcExterno.insets = new Insets(0, 0, 40, 0);
        add(panelControles, gbcExterno);
    }

    private JPanel crearPanelControles(IntConsumer alIniciar) {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(FONDO_PANEL);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.setColor(VERDE_TOXICO_OSCURO);
                g2d.setStroke(new java.awt.BasicStroke(2));
                g2d.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                g2d.dispose();
            }
        };
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.gridx = 0;

        JLabel titulo = new JLabel("CONFIGURAR BATALLA", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(VERDE_TOXICO);
        gbc.gridy = 0;
        gbc.insets = new Insets(15, 10, 5, 10);
        panel.add(titulo, gbc);

        JLabel etiquetaTamano = new JLabel("Tamano de cada equipo");
        etiquetaTamano.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        etiquetaTamano.setForeground(Color.WHITE);
        gbc.gridy = 1;
        gbc.insets = new Insets(8, 10, 4, 10);
        panel.add(etiquetaTamano, gbc);

        SpinnerNumberModel modelo = new SpinnerNumberModel(
                ConstantesJuego.TAMANO_EQUIPO,
                ConstantesJuego.TAMANO_EQUIPO,
                ConstantesJuego.TAMANO_EQUIPO_MAX,
                1);
        JSpinner spinnerTamano = new JSpinner(modelo);
        spinnerTamano.setPreferredSize(new Dimension(90, 32));
        spinnerTamano.setFont(new Font("Segoe UI", Font.BOLD, 16));
        spinnerTamano.setBorder(BorderFactory.createLineBorder(VERDE_TOXICO_OSCURO, 1));
        gbc.gridy = 2;
        gbc.insets = new Insets(4, 10, 15, 10);
        panel.add(spinnerTamano, gbc);

        BotonToxico botonIniciar = new BotonToxico("INICIAR BATALLA");
        botonIniciar.addActionListener(e -> alIniciar.accept((Integer) spinnerTamano.getValue()));
        gbc.gridy = 3;
        gbc.insets = new Insets(5, 10, 20, 10);
        panel.add(botonIniciar, gbc);

        return panel;
    }

    private void cargarImagenFondo() {
        try {
            imgFondo = ImageIO.read(getClass().getResource("/mutantes/imagenes/menu.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudo cargar la imagen del menu: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (imgFondo != null) {
            g2d.drawImage(imgFondo, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private static class BotonToxico extends JButton {

        BotonToxico(String texto) {
            super(texto);
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setForeground(Color.BLACK);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setPreferredSize(new Dimension(220, 45));
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color colorBase = getModel().isRollover() ? VERDE_TOXICO.brighter() : VERDE_TOXICO;
            if (getModel().isPressed()) {
                colorBase = VERDE_TOXICO_OSCURO;
            }

            g2d.setColor(colorBase);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2d.setColor(VERDE_TOXICO_OSCURO);
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            g2d.dispose();
            super.paintComponent(g);
        }
    }
}