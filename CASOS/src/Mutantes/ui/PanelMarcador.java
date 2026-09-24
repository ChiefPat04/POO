package mutantes.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import mutantes.game.CampoDeBatalla;
import mutantes.game.Equipo;
import mutantes.game.ObservadorBatalla;
import mutantes.model.Mutante;
import mutantes.model.poderes.TipoEfecto;

public class PanelMarcador extends JPanel implements ObservadorBatalla {

    private static final Color COLOR_FONDO = new Color(25, 25, 35);
    private static final Color COLOR_EQUIPO_A = new Color(220, 60, 60);
    private static final Color COLOR_EQUIPO_B = new Color(60, 130, 220);
    private static final Color COLOR_GANADOR = new Color(250, 210, 60);
    private static final int ESPACIADO_ICONO_PODER = 22;
    private static final int TAMANO_ICONO_PODER = 16;

    private final CampoDeBatalla campo;
    private final List<IconoPoder> iconosDibujados = new ArrayList<>();

    public PanelMarcador(CampoDeBatalla campo) {
        this.campo = campo;
        setBackground(COLOR_FONDO);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    @Override
    public void alActualizarEstado() {
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent evento) {
        for (IconoPoder icono : iconosDibujados) {
            if (icono.area.contains(evento.getPoint())) {
                return "<html><b>" + EfectosVisuales.nombreLegibleParaEfecto(icono.tipo) + "</b><br>"
                        + EfectosVisuales.descripcionParaEfecto(icono.tipo) + "</html>";
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        iconosDibujados.clear();

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Equipo equipoA = campo.getEquipoA();
        Equipo equipoB = campo.getEquipoB();

        int mitad = getWidth() / 2;

        dibujarMarcadorEquipo(g2d, "EQUIPO ROJO", equipoA, COLOR_EQUIPO_A, 20);
        dibujarMarcadorEquipo(g2d, "EQUIPO AZUL", equipoB, COLOR_EQUIPO_B, mitad + 20);

        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.drawLine(mitad, 10, mitad, getHeight() - 10);

        if (campo.hayGanador() && campo.getGanador() != null) {
            String texto = "GANADOR: EQUIPO " + campo.getGanador().getColor();
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
            g2d.setColor(COLOR_GANADOR);
            int anchoTexto = g2d.getFontMetrics().stringWidth(texto);
            g2d.drawString(texto, (getWidth() - anchoTexto) / 2, getHeight() - 8);
        }
    }

    private void dibujarMarcadorEquipo(Graphics2D g2d, String nombre, Equipo equipo, Color color, int x) {
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
        g2d.setColor(color);
        g2d.drawString(nombre, x, 22);

        g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawString("Vivos: " + equipo.getVivos() + "   Muertos: " + equipo.getMuertos(), x, 40);

        dibujarPoderesEquipo(g2d, equipo, x, 62);
    }

    private void dibujarPoderesEquipo(Graphics2D g2d, Equipo equipo, int x, int y) {
        Set<TipoEfecto> poderesUnicos = obtenerPoderesUnicos(equipo);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        int posX = x;

        for (TipoEfecto tipo : poderesUnicos) {
            g2d.setColor(EfectosVisuales.colorParaEfecto(tipo));
            g2d.fillOval(posX, y - 12, TAMANO_ICONO_PODER, TAMANO_ICONO_PODER);

            g2d.setColor(Color.BLACK);
            g2d.drawString(EfectosVisuales.simboloParaEfecto(tipo), posX + 4, y);

            iconosDibujados.add(new IconoPoder(tipo, new Rectangle(posX, y - 12, TAMANO_ICONO_PODER, TAMANO_ICONO_PODER)));

            posX += ESPACIADO_ICONO_PODER;
        }
    }

    private Set<TipoEfecto> obtenerPoderesUnicos(Equipo equipo) {
        Set<TipoEfecto> poderes = new LinkedHashSet<>();
        for (Mutante mutante : equipo.getMutantes()) {
            if (mutante.getPoder() != null) {
                poderes.add(mutante.getPoder().getTipoEfecto());
            }
        }
        return poderes;
    }

    private static class IconoPoder {
        private final TipoEfecto tipo;
        private final Rectangle area;

        IconoPoder(TipoEfecto tipo, Rectangle area) {
            this.tipo = tipo;
            this.area = area;
        }
    }
}