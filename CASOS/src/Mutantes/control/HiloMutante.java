package mutantes.control;

import java.util.ArrayList;
import java.util.List;
import mutantes.constants.ConstantesJuego;
import mutantes.game.CampoDeBatalla;
import mutantes.game.Equipo;
import mutantes.model.Mutante;

public class HiloMutante implements Runnable {

    private final Mutante mutante;
    private final CampoDeBatalla campo;
    private final GestorCombate gestor;

    public HiloMutante(Mutante mutante, CampoDeBatalla campo, GestorCombate gestor) {
        this.mutante = mutante;
        this.campo = campo;
        this.gestor = gestor;
    }

    @Override
    public void run() {
        int[] dimensiones = campo.getDimensiones();

        while (mutante.estaVivo() && !Thread.currentThread().isInterrupted()) {
            mutante.moverse(dimensiones[0], dimensiones[1]);
            resolverEncuentrosCercanos();
            dormir();
        }
    }

    private void resolverEncuentrosCercanos() {
        for (Mutante enemigo : detectarEnemigosEnRadio()) {
            if (enemigo.estaVivo()) {
                gestor.resolverEncuentro(mutante, enemigo);
            }
        }
    }

    private List<Mutante> detectarEnemigosEnRadio() {
        Equipo equipoEnemigo = campo.getEquipoEnemigoDe(mutante);
        int radio = campo.getRadioDeteccion();

        List<Mutante> enEsteRadio = new ArrayList<>();

        for (Mutante enemigo : equipoEnemigo.getMutantes()) {
            if (enemigo.estaVivo() && dentroDelRadio(enemigo, radio)) {
                enEsteRadio.add(enemigo);
            }
        }

        return enEsteRadio;
    }

    private boolean dentroDelRadio(Mutante enemigo, int radio) {
        int dx = mutante.getPosicionX() - enemigo.getPosicionX();
        int dy = mutante.getPosicionY() - enemigo.getPosicionY();
        double distancia = Math.sqrt((dx * dx) + (dy * dy));
        return distancia <= radio;
    }

    private void dormir() {
        try {
            Thread.sleep(ConstantesJuego.INTERVALO_MOVIMIENTO_MS);
        } catch (InterruptedException excepcion) {
            Thread.currentThread().interrupt();
        }
    }
}
