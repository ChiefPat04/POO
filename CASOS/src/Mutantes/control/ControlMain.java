package mutantes.control;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mutantes.constants.ConstantesJuego;
import mutantes.game.CampoDeBatalla;
import mutantes.game.Equipo;
import mutantes.model.Mutante;

public class ControlMain {

    public static void main(String[] args) throws InterruptedException {
        CampoDeBatalla campo = new CampoDeBatalla(800, 600);
        campo.crearEquipos(3);

        // Solo para esta prueba: forzamos a que arranquen cerca,
        // para confirmar rapido que la deteccion y el combate SI disparan.
        // Las zonas base reales (lejos entre si) se siguen usando en el juego real.
        for (Mutante mutante : campo.getEquipoA().getMutantes()) {
            mutante.reposicionar(390, 300);
        }
        for (Mutante mutante : campo.getEquipoB().getMutantes()) {
            mutante.reposicionar(410, 300);
        }

        GestorCombate gestor = new GestorCombate(campo);

        ExecutorService pool = Executors.newFixedThreadPool(ConstantesJuego.CANTIDAD_HILOS_POOL);

        for (Mutante mutante : campo.getEquipoA().getMutantes()) {
            pool.submit(new HiloMutante(mutante, campo, gestor));
        }
        for (Mutante mutante : campo.getEquipoB().getMutantes()) {
            pool.submit(new HiloMutante(mutante, campo, gestor));
        }

        System.out.println("Combate iniciado (mutantes forzados a nacer cerca), corriendo por 5 segundos...");
        Thread.sleep(5000);

        pool.shutdownNow();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println();
        System.out.println("--- Estado final ---");
        imprimirEquipo("Equipo A", campo.getEquipoA());
        imprimirEquipo("Equipo B", campo.getEquipoB());

        System.out.println("Hay ganador: " + campo.hayGanador());
        if (campo.hayGanador() && campo.getGanador() != null) {
            System.out.println("Gano el equipo: " + campo.getGanador().getColor());
        }
    }

    private static void imprimirEquipo(String etiqueta, Equipo equipo) {
        System.out.println(etiqueta + " - vivos: " + equipo.getVivos() + ", muertos: " + equipo.getMuertos());
        for (Mutante mutante : equipo.getMutantes()) {
            System.out.println("  " + mutante.getNombre()
                    + " | energia=" + mutante.getEnergia()
                    + " | vivo=" + mutante.estaVivo()
                    + " | pos=(" + mutante.getPosicionX() + "," + mutante.getPosicionY() + ")");
        }
    }
}