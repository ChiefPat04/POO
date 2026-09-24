package mutantes.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import mutantes.constants.ConstantesJuego;
import mutantes.game.CampoDeBatalla;
import mutantes.game.ObservadorBatalla;
import mutantes.model.Mutante;
import mutantes.model.poderes.TipoEfecto;

public class PanelCampoBatalla extends JPanel implements ObservadorBatalla {

    private static final int RADIO_DIBUJO = 30;
    private static final long DURACION_EFECTO_GOLPE_MS = 200;
    private static final long DURACION_EFECTO_PODER_MS = 400;
    private static final int ANCHO_BARRA_ENERGIA = 30;
    private static final int ALTO_BARRA_ENERGIA = 4;

    private final CampoDeBatalla campo;
    private BufferedImage imgEquipoA;
    private BufferedImage imgEquipoB;
    private BufferedImage imgFondo;

    public PanelCampoBatalla(CampoDeBatalla campo) {
        this.campo = campo;
        setBackground(Color.WHITE);
        cargarImagenes();
    }

    private void cargarImagenes() {
        try {
            imgEquipoA = ImageIO.read(getClass().getResource("/mutantes/imagenes/mutante_equipoA.png"));
            imgEquipoB = ImageIO.read(getClass().getResource("/mutantes/imagenes/mutante_equipoB.png"));
            imgFondo = ImageIO.read(getClass().getResource("/mutantes/imagenes/fondo.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("No se pudieron cargar las imagenes: " + e.getMessage());
        }
    }

    @Override
    public void alActualizarEstado() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (imgFondo != null) {
            g2d.drawImage(imgFondo, 0, 0, getWidth(), getHeight(), this);
        }

        dibujarEquipo(g2d, campo.getEquipoA().getMutantes(), imgEquipoA);
        dibujarEquipo(g2d, campo.getEquipoB().getMutantes(), imgEquipoB);
    }

    private void dibujarEquipo(Graphics g, List<Mutante> mutantes, BufferedImage imagen) {
        for (Mutante mutante : mutantes) {
            if (!mutante.estaVivo()) {
                continue;
            }

            int x = mutante.getPosicionX();
            int y = mutante.getPosicionY();

            if (imagen != null) {
                g.drawImage(imagen, x - RADIO_DIBUJO, y - RADIO_DIBUJO,
                        RADIO_DIBUJO * 2, RADIO_DIBUJO * 2, this);
            } else {
                g.setColor(Color.GRAY);
                g.fillOval(x - RADIO_DIBUJO, y - RADIO_DIBUJO, RADIO_DIBUJO * 2, RADIO_DIBUJO * 2);
            }

            boolean golpeReciente = System.currentTimeMillis() - mutante.getUltimoGolpeMillis() < DURACION_EFECTO_GOLPE_MS;
            if (golpeReciente) {
                g.setColor(Color.YELLOW);
                g.drawOval(x - RADIO_DIBUJO - 3, y - RADIO_DIBUJO - 3, (RADIO_DIBUJO + 3) * 2, (RADIO_DIBUJO + 3) * 2);
            }

            dibujarBarraEnergia(g, mutante, x, y);
            dibujarEfectoPoder(g, mutante, x, y);
        }
    }

    private void dibujarBarraEnergia(Graphics g, Mutante mutante, int x, int y) {
        int barraX = x - ANCHO_BARRA_ENERGIA / 2;
        int barraY = y - RADIO_DIBUJO - 10;

        double porcentaje = mutante.getEnergia() / (double) ConstantesJuego.ENERGIA_INICIAL;
        int anchoActual = (int) (ANCHO_BARRA_ENERGIA * porcentaje);

        g.setColor(Color.DARK_GRAY);
        g.drawRoundRect(barraX, barraY, ANCHO_BARRA_ENERGIA, ALTO_BARRA_ENERGIA, 4, 4);

        g.setColor(porcentaje > 0.5 ? Color.GREEN : (porcentaje > 0.2 ? Color.ORANGE : Color.RED));
        g.fillRoundRect(barraX, barraY, anchoActual, ALTO_BARRA_ENERGIA, 4, 4);
    }

    private void dibujarEfectoPoder(Graphics g, Mutante mutante, int x, int y) {
        TipoEfecto tipo = mutante.getUltimoPoderUsado();
        if (tipo == null) {
            return;
        }

        boolean poderReciente = System.currentTimeMillis() - mutante.getUltimoPoderMillis() < DURACION_EFECTO_PODER_MS;
        if (!poderReciente) {
            return;
        }

        g.setColor(EfectosVisuales.colorParaEfecto(tipo));
        g.drawOval(x - RADIO_DIBUJO - 6, y - RADIO_DIBUJO - 6, (RADIO_DIBUJO + 6) * 2, (RADIO_DIBUJO + 6) * 2);
        g.drawString(EfectosVisuales.simboloParaEfecto(tipo), x - 4, y - RADIO_DIBUJO - 15);
    }
}