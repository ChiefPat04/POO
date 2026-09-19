package mutantes.model;

import mutantes.model.poderes.PoderFuerza;
import mutantes.model.poderes.PoderRegeneracion;

public class ModelMain {

    public static void main(String[] args) {
        Mutante mutante = new Mutante(1, "Wolverine", 2, new PoderFuerza(3));

        System.out.println("Energia inicial: " + mutante.getEnergia());
        System.out.println("Esta vivo: " + mutante.estaVivo());

        mutante.recibirDano(40);
        System.out.println("Energia despues de 40 de dano: " + mutante.getEnergia());

        mutante.recibirDano(80);
        System.out.println("Energia despues de otros 80 de dano: " + mutante.getEnergia());
        System.out.println("Esta vivo: " + mutante.estaVivo());

        mutante.recibirDano(50);
        System.out.println("Energia intentando danar a un muerto: " + mutante.getEnergia());

        System.out.println("--- Prueba de ataque con poder (Mutante.atacar) ---");

        Mutante sanador = new Mutante(2, "Curador", 1, new PoderRegeneracion(4));
        Mutante enemigo = new Mutante(3, "Enemigo de prueba", 1, new PoderFuerza(2));

        System.out.println("Energia inicial del sanador: " + sanador.getEnergia());
        System.out.println("Energia inicial del enemigo: " + enemigo.getEnergia());
        System.out.println("Nivel de dano del poder del sanador antes de atacar: " + sanador.getPoder().getDano());

        sanador.atacar(enemigo);

        System.out.println("Energia del enemigo tras el ataque (deberia bajar): " + enemigo.getEnergia());
        System.out.println("Energia del sanador tras el ataque (deberia subir, se curo): " + sanador.getEnergia());
        System.out.println("Nivel de dano del poder del sanador tras el ataque (deberia subir a 5): " + sanador.getPoder().getDano());
    }
}

// Con la salida de estos datos confirmamos que: el poder hace daño real al enemigo, cura de verdad al dueño (algo que antes no pasaba),
//  y el nivel del poder sube automáticamente solo cuando el golpe fue efectivo — las tres reglas del enunciado funcionando juntas, 
// sin que ninguna clase tenga que "acordarse" manualmente de aplicarlas todas.