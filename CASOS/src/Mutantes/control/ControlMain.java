package mutantes.control;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mutantes.constants.ConstantesJuego;
import mutantes.game.CampoDeBatalla;
import mutantes.game.Equipo;
import mutantes.model.Mutante;

public class ControlMain {

    private static ExecutorService pool;

    public static void main(String[] args) throws InterruptedException {
        CampoDeBatalla campo = new CampoDeBatalla(800, 600);
        campo.crearEquipos(11);
        // sin reposicionar manualmente esta vez, que usen sus zonas base reales

        GestorCombate gestor = new GestorCombate(campo);

        int totalMutantes = campo.getEquipoA().getMutantes().size() + campo.getEquipoB().getMutantes().size();
        pool = Executors.newFixedThreadPool(totalMutantes);

        
        for (Mutante mutante : campo.getEquipoA().getMutantes()) {
            pool.submit(new HiloMutante(mutante, campo, gestor));
        }
        for (Mutante mutante : campo.getEquipoB().getMutantes()) {
            pool.submit(new HiloMutante(mutante, campo, gestor));
        }

        System.out.println("Combate iniciado, corriendo por 3 minutos");
        Thread.sleep(30000);  

        pool.shutdownNow();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        boolean hayGanador = campo.hayGanador();

        System.out.println();
        System.out.println("--- Estado final ---");
        imprimirEquipo("Equipo A", campo.getEquipoA());
        imprimirEquipo("Equipo B", campo.getEquipoB());

        System.out.println("Hay ganador: " + hayGanador);
        if (hayGanador && campo.getGanador() != null) {
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