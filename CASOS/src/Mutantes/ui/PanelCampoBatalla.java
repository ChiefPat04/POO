package mutantes.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import javax.swing.JPanel;
import mutantes.game.CampoDeBatalla;
import mutantes.game.ObservadorBatalla;
import mutantes.model.Mutante;

public class PanelCampoBatalla extends JPanel implements ObservadorBatalla {

    private static final int RADIO_DIBUJO = 12;
    private static final long DURACION_EFECTO_GOLPE_MS = 200;

    private final CampoDeBatalla campo;

    public PanelCampoBatalla(CampoDeBatalla campo) {
        this.campo = campo;
        setBackground(Color.WHITE);
    }

    @Override
    public void alActualizarEstado() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //por convención de Swing llamar primero a la versión original del método (super) antes de agregar tu propio dibujo.
        dibujarEquipo(g, campo.getEquipoA().getMutantes(), Color.RED);
        dibujarEquipo(g, campo.getEquipoB().getMutantes(), Color.BLUE);
    }

    private void dibujarEquipo(Graphics g, List<Mutante> mutantes, Color color) {
        for (Mutante mutante : mutantes) {
            if (!mutante.estaVivo()) {
                continue;
            }

            int x = mutante.getPosicionX();
            int y = mutante.getPosicionY();

            g.setColor(color);
            g.fillOval(x - RADIO_DIBUJO, y - RADIO_DIBUJO, RADIO_DIBUJO * 2, RADIO_DIBUJO * 2);

            boolean golpeReciente = System.currentTimeMillis() - mutante.getUltimoGolpeMillis() < DURACION_EFECTO_GOLPE_MS;
            if (golpeReciente) {
                g.setColor(Color.YELLOW);
                g.drawOval(x - RADIO_DIBUJO - 3, y - RADIO_DIBUJO - 3, (RADIO_DIBUJO + 3) * 2, (RADIO_DIBUJO + 3) * 2);
            }
        }
    }
}

//la UI solo reacciona, nunca calcula lógica.