package mutantes.game;

public class GameMain {

    public static void main(String[] args) {
        CampoDeBatalla campo = new CampoDeBatalla(800, 600);
        campo.crearEquipos(3);

        System.out.println("Equipo A - vivos: " + campo.getEquipoA().getVivos());
        System.out.println("Equipo B - vivos: " + campo.getEquipoB().getVivos());
        System.out.println("Hay ganador todavia: " + campo.hayGanador());
        
        System.out.println("--- Nombres generados para el equipo A ---");
        for (var mutante : campo.getEquipoA().getMutantes()) {
            System.out.println(mutante.getNombre() + " (defensa " + mutante.getDefensa() + ")");
        }

        System.out.println("--- Matando a todo el quipo A manualmente ---");
        for (var mutante : campo.getEquipoA().getMutantes()) {
            mutante.recibirDano(200);
        }

        System.out.println("Hay ganador ahora: " + campo.hayGanador());

        if (campo.hayGanador()) {
            Equipo ganador = campo.getGanador();
            System.out.println("Gano el equipo de color: " + ganador.getColor());
        }
    }
}