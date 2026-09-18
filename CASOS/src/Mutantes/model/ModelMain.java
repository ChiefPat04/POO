package Mutantes.model;

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
    }
}