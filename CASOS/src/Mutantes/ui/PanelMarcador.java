package mutantes.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
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

    private static final Color COLOR_FONDO = new Color(15, 10, 15);
    private static final Color COLOR_EQUIPO_A = new Color(230, 70, 70);
    private static final Color COLOR_EQUIPO_B = new Color(70, 150, 230);
    private static final Color COLOR_GANADOR = new Color(120, 220, 60);
    private static final int ESPACIADO_ICONO_PODER = 22;
    private static final int TAMANO_ICONO_PODER = 16;

    private final CampoDeBatalla campo;
    private final List<IconoPoder> iconosDibujados = new ArrayList<>();
    private Font fuentePixelGrande;
    private Font fuentePixelChica;

    public PanelMarcador(CampoDeBatalla campo) {
        this.campo = campo;
        setBackground(COLOR_FONDO);
        ToolTipManager.sharedInstance().registerComponent(this);
        cargarFuentePixel();
    }

    private void cargarFuentePixel() {
        try (InputStream in = getClass().getResourceAsStream("/mutantes/fuentes/PressStart2P.ttf")) {
            if (in == null) {
                throw new IOException("Archivo de fuente no encontrado");
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, in);
            fuentePixelGrande = base.deriveFont(Font.PLAIN, 14f);
            fuentePixelChica = base.deriveFont(Font.PLAIN, 9f);
        } catch (IOException | FontFormatException e) {
            System.err.println("No se pudo cargar la fuente pixel, usando fuente por defecto: " + e.getMessage());
            fuentePixelGrande = new Font("Monospaced", Font.BOLD, 15);
            fuentePixelChica = new Font("Monospaced", Font.BOLD, 11);
        }
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
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Equipo equipoA = campo.getEquipoA();
        Equipo equipoB = campo.getEquipoB();

        int mitad = getWidth() / 2;

        dibujarPlacaEquipo(g2d, equipoA, COLOR_EQUIPO_A, 10, 5, mitad - 20);
        dibujarPlacaEquipo(g2d, equipoB, COLOR_EQUIPO_B, mitad + 10, 5, mitad - 20);

        if (campo.hayGanador() && campo.getGanador() != null) {
            String texto = "GANADOR: EQUIPO " + campo.getGanador().getColor();
            g2d.setFont(fuentePixelChica);
            g2d.setColor(COLOR_GANADOR);
            int anchoTexto = g2d.getFontMetrics().stringWidth(texto);
            g2d.drawString(texto, (getWidth() - anchoTexto) / 2, getHeight() - 8);
        }
    }

    private void dibujarPlacaEquipo(Graphics2D g2d, Equipo equipo, Color color, int x, int y, int ancho) {
        int alto = getHeight() - 15;

        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 25));
        g2d.fillRoundRect(x, y, ancho, alto, 12, 12);
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 140));
        g2d.drawRoundRect(x, y, ancho, alto, 12, 12);

        String nombre = equipo.getSimbolo() + " EQUIPO " + equipo.getColor();

        g2d.setFont(fuentePixelGrande);
        dibujarTextoConSombra(g2d, nombre, x + 12, y + 22, color);

        g2d.setFont(fuentePixelChica);
        String stats = "VIVOS " + equipo.getVivos() + "  MUERTOS " + equipo.getMuertos();
        dibujarTextoConSombra(g2d, stats, x + 12, y + 44, Color.LIGHT_GRAY);

        dibujarPoderesEquipo(g2d, equipo, x + 12, y + 66);
    }

    private void dibujarTextoConSombra(Graphics2D g2d, String texto, int x, int y, Color color) {
        g2d.setColor(Color.BLACK);
        g2d.drawString(texto, x + 1, y + 1);
        g2d.setColor(color);
        g2d.drawString(texto, x, y);
    }

    private void dibujarPoderesEquipo(Graphics2D g2d, Equipo equipo, int x, int y) {
        Set<TipoEfecto> poderesUnicos = obtenerPoderesUnicos(equipo);

        g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
        int posX = x;

        for (TipoEfecto tipo : poderesUnicos) {
            g2d.setColor(EfectosVisuales.colorParaEfecto(tipo));
            g2d.fillOval(posX, y - 12, TAMANO_ICONO_PODER, TAMANO_ICONO_PODER);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(posX, y - 12, TAMANO_ICONO_PODER, TAMANO_ICONO_PODER);
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