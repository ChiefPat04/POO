package mutantes.model;
import mutantes.model.poderes.PoderMutante;
import mutantes.model.poderes.PoderFuerza;
import mutantes.model.poderes.PoderVelocidad;
import mutantes.model.poderes.PoderRegeneracion;

public class ModelMain {

    public static void main(String[] args) {
        Mutante mutante = new Mutante(1, "Wolverine", 2);

        System.out.println("Energia inicial: " + mutante.getEnergia());
        System.out.println("Esta vivo: " + mutante.estaVivo());

        mutante.recibirDano(40);
        System.out.println("Energia despues de 40 de dano: " + mutante.getEnergia());

        mutante.recibirDano(80);
        System.out.println("Energia despues de otros 80 de dano: " + mutante.getEnergia());
        System.out.println("Esta vivo: " + mutante.estaVivo());

        mutante.recibirDano(50);
        System.out.println("Energia intentando danar a un muerto: " + mutante.getEnergia());

        System.out.println("--- Prueba de los 3 tipos de poder ---");

        Mutante enemigo = new Mutante(3, "Enemigo de prueba", 1);

        PoderMutante fuerza = new PoderFuerza(3);
        PoderMutante velocidad = new PoderVelocidad(3);
        PoderMutante regeneracion = new PoderRegeneracion(3);

        fuerza.aplicarEfecto(enemigo);
        System.out.println("Energia tras Fuerza (dano 3): " + enemigo.getEnergia());

        velocidad.aplicarEfecto(enemigo);
        System.out.println("Energia tras Velocidad (dano 3): " + enemigo.getEnergia());

        regeneracion.aplicarEfecto(enemigo);
        System.out.println("Energia tras Regeneracion (dano 3/2 = 1): " + enemigo.getEnergia());
    }
}