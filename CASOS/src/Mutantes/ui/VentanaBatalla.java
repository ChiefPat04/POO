package mutantes.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;
import mutantes.constants.ConstantesJuego;
import mutantes.control.GestorCombate;
import mutantes.control.HiloMutante;
import mutantes.game.CampoDeBatalla;
import mutantes.model.Mutante;

public class VentanaBatalla extends JFrame {

    private CampoDeBatalla campo;
    private ExecutorService pool;
    private Timer timerRefresco;
    private final JButton botonNuevaPartida;

    public VentanaBatalla() {
        setTitle("Batalla de Mutantes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLayout(new BorderLayout());

        botonNuevaPartida = new JButton("Nueva Partida");
        botonNuevaPartida.addActionListener(evento -> iniciarPartida());

        iniciarPartida();
        setVisible(true);
    }

    private void iniciarPartida() {
        detenerPartidaAnterior();

        int tamano = pedirTamanoEquipo();

        campo = new CampoDeBatalla(760, 560);
        campo.crearEquipos(tamano);

        PanelCampoBatalla panelCampo = new PanelCampoBatalla(campo);
        PanelMarcador panelMarcador = new PanelMarcador(campo);
        panelMarcador.setPreferredSize(new Dimension(760, 80));

        campo.agregarObservador(panelCampo);
        campo.agregarObservador(panelMarcador);

        getContentPane().removeAll();
        add(panelMarcador, BorderLayout.NORTH);
        add(panelCampo, BorderLayout.CENTER);
        add(botonNuevaPartida, BorderLayout.SOUTH);
        revalidate();
        repaint();

        GestorCombate gestor = new GestorCombate(campo);
        pool = Executors.newFixedThreadPool(ConstantesJuego.CANTIDAD_HILOS_POOL);

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
            }
        });
        timerRefresco.start();
    }

    private void detenerPartidaAnterior() {
        if (timerRefresco != null) {
            timerRefresco.stop();
        }
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    private int pedirTamanoEquipo() {
        while (true) {
            String entrada = JOptionPane.showInputDialog(this,
                    "Tamano de cada equipo (" + ConstantesJuego.TAMANO_EQUIPO + " a "
                            + ConstantesJuego.TAMANO_EQUIPO_MAX + "):");

            if (entrada == null) {
                System.exit(0);
            }

            try {
                int tamano = Integer.parseInt(entrada.trim());
                if (tamano >= ConstantesJuego.TAMANO_EQUIPO && tamano <= ConstantesJuego.TAMANO_EQUIPO_MAX) {
                    return tamano;
                }
            } catch (NumberFormatException excepcion) {
                // se vuelve a pedir, no hacemos nada aqui
            }
        }
    }
}