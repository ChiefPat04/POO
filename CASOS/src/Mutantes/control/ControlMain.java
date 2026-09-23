package mutantes.control;

import mutantes.model.Mutante;
import mutantes.model.poderes.PoderFuerza;

public class ControlMain {

    public static void main(String[] args) {
        GestorCombate gestor = new GestorCombate();

        Mutante m1 = new Mutante(1, "Mutante Uno", 2, new PoderFuerza(3));
        Mutante m2 = new Mutante(2, "Mutante Dos", 3, new PoderFuerza(3));

        System.out.println("Antes del combate:");
        System.out.println("M1 energia: " + m1.getEnergia());
        System.out.println("M2 energia: " + m2.getEnergia());

        for (int i = 1; i <= 5; i++) {
            gestor.resolverEncuentro(m1, m2);
            System.out.println("Ronda " + i + " -> M1: " + m1.getEnergia() + " | M2: " + m2.getEnergia());
        }

        System.out.println();
        System.out.println("--- Prueba de proteccion contra combate duplicado ---");
        System.out.println("Llamando resolverEncuentro(m1, m2) y resolverEncuentro(m2, m1) 'al mismo tiempo' (secuencial aqui, luego con hilos reales):");

        int energiaM1Antes = m1.getEnergia();
        int energiaM2Antes = m2.getEnergia();

        gestor.resolverEncuentro(m1, m2);
        gestor.resolverEncuentro(m2, m1);

        System.out.println("M1 cambio: " + (energiaM1Antes - m1.getEnergia()));
        System.out.println("M2 cambio: " + (energiaM2Antes - m2.getEnergia()));
    }
}