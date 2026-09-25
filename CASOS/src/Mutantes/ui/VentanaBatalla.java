package mutantes.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import mutantes.constants.ConstantesJuego;
import mutantes.control.GestorCombate;
import mutantes.control.HiloMutante;
import mutantes.game.CampoDeBatalla;
import mutantes.model.Mutante;

public class VentanaBatalla extends JFrame {

    private static final String CARTA_MENU = "menu";
    private static final String CARTA_JUEGO = "juego";

    private final CardLayout cardLayout;
    private final JPanel contenedor;
    private final JButton botonNuevaPartida;
    private final JButton botonAyuda;
    private final ReproductorMusica musica = new ReproductorMusica();

    private CampoDeBatalla campo;
    private ExecutorService pool;
    private Timer timerRefresco;

    public VentanaBatalla() {
        setTitle("Batalla de Mutantes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLayout(new BorderLayout());

        cardLayout = new CardLayout();
        contenedor = new JPanel(cardLayout);

        PanelMenu panelMenu = new PanelMenu(this::iniciarPartida);
        contenedor.add(panelMenu, CARTA_MENU);

        add(contenedor, BorderLayout.CENTER);

        botonNuevaPartida = new JButton("Volver al Menu");
        botonNuevaPartida.addActionListener(evento -> mostrarMenu());

        botonAyuda = new JButton("? Guia de Poderes");
        botonAyuda.addActionListener(evento -> new DialogoAyudaPoderes(this).setVisible(true));

        cardLayout.show(contenedor, CARTA_MENU);
        setVisible(true);

        musica.reproducirEnBucle("/mutantes/imagenes/musica_menu.wav");
    }

    private void mostrarMenu() {
        detenerPartidaAnterior();
        cardLayout.show(contenedor, CARTA_MENU);
    }

    private void iniciarPartida(int tamano) {
        detenerPartidaAnterior();

        campo = new CampoDeBatalla(700, 500);
        campo.crearEquipos(tamano);

        PanelCampoBatalla panelCampo = new PanelCampoBatalla(campo);
        PanelMarcador panelMarcador = new PanelMarcador(campo);
        panelMarcador.setPreferredSize(new Dimension(760, 90));

        campo.agregarObservador(panelCampo);
        campo.agregarObservador(panelMarcador);

        JPanel panelBotones = new JPanel();
        panelBotones.add(botonNuevaPartida);
        panelBotones.add(botonAyuda);

        JPanel panelJuego = new JPanel(new BorderLayout());
        panelJuego.add(panelMarcador, BorderLayout.NORTH);
        panelJuego.add(panelCampo, BorderLayout.CENTER);
        panelJuego.add(panelBotones, BorderLayout.SOUTH);

        contenedor.add(panelJuego, CARTA_JUEGO);
        cardLayout.show(contenedor, CARTA_JUEGO);

        GestorCombate gestor = new GestorCombate(campo);

        int totalMutantes = campo.getEquipoA().getMutantes().size() + campo.getEquipoB().getMutantes().size();
        pool = Executors.newFixedThreadPool(totalMutantes);

        for (Mutante mutante : campo.getEquipoA().getMutantes()) {
            pool.submit(new HiloMutante(mutante, campo, gestor));
        }
        for (Mutante mutante : campo.getEquipoB().getMutantes()) {
            pool.submit(new HiloMutante(mutante, campo, gestor));
        }

        timerRefresco = new Timer(ConstantesJuego.INTERVALO_MOVIMIENTO_MS, evento -> {
            campo.notificarObservadores();
            if (campo.hayGanador()) {
                timerRefresco.stop();
                pool.shutdownNow();
                anunciarGanadorYPreguntarReinicio();
            }
        });
        timerRefresco.start();
    }

    private void anunciarGanadorYPreguntarReinicio() {
        String nombreGanador = campo.getGanador() != null ? campo.getGanador().getColor().toString() : "Nadie";

        int opcion = JOptionPane.showConfirmDialog(this,
                "Gano el equipo " + nombreGanador + ". Deseas volver al menu?",
                "Fin de la partida",
                JOptionPane.YES_NO_OPTION);

        if (opcion == JOptionPane.YES_OPTION) {
            mostrarMenu();
        }
    }

    private void detenerPartidaAnterior() {
        if (timerRefresco != null) {
            timerRefresco.stop();
        }
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}