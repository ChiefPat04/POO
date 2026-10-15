package mutantes.control;

import java.util.HashSet;
import java.util.Set;
import mutantes.constants.ConstantesJuego;
import mutantes.game.CampoDeBatalla;
import mutantes.game.Equipo;
import mutantes.model.Mutante;

public class HiloMutante implements Runnable {

    private final Mutante mutante;
    private final CampoDeBatalla campo;
    private final GestorCombate gestor;
    private final Set<Integer> enemigosEnContacto = new HashSet<>();

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
        Equipo equipoEnemigo = campo.getEquipoEnemigoDe(mutante);
        int radio = campo.getRadioDeteccion();
        // HashSet normal (no concurrente): solo este hilo lee y escribe esta
        // coleccion, ningun otro hilo la toca, asi que no necesita proteccion.
        // Contraste con GestorCombate.paresEnResolucion, que SI la necesita porque
        // dos hilos distintos pueden tocarlo.
        Set<Integer> enemigosAhoraEnRadio = new HashSet<>();

        for (Mutante enemigo : equipoEnemigo.getMutantes()) {
            if (!enemigo.estaVivo()) {
                continue;
            }

            if (!dentroDelRadio(enemigo, radio)) {
                continue;
            }

            enemigosAhoraEnRadio.add(enemigo.getId());

            boolean esEncuentroNuevo = !enemigosEnContacto.contains(enemigo.getId());
            if (esEncuentroNuevo) {
                gestor.resolverEncuentro(mutante, enemigo);
            }
        }

        enemigosEnContacto.clear();
        enemigosEnContacto.addAll(enemigosAhoraEnRadio);
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
