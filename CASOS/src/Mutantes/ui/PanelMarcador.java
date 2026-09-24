package mutantes.ui;

import java.awt.Graphics;
import javax.swing.JPanel;
import mutantes.game.CampoDeBatalla;
import mutantes.game.Equipo;
import mutantes.game.ObservadorBatalla;

public class PanelMarcador extends JPanel implements ObservadorBatalla {

    private final CampoDeBatalla campo;

    public PanelMarcador(CampoDeBatalla campo) {
        this.campo = campo;
    }

    @Override
    public void alActualizarEstado() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Equipo equipoA = campo.getEquipoA();
        Equipo equipoB = campo.getEquipoB();

        g.drawString("Equipo ROJO - vivos: " + equipoA.getVivos() + " | muertos: " + equipoA.getMuertos(), 10, 20);
        g.drawString("Equipo AZUL - vivos: " + equipoB.getVivos() + " | muertos: " + equipoB.getMuertos(), 10, 40);

        if (campo.hayGanador() && campo.getGanador() != null) {
            g.drawString("GANADOR: Equipo " + campo.getGanador().getColor(), 10, 60);
        }
    }
}