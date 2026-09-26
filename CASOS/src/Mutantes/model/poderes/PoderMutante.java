package mutantes.model.poderes;

import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;

public abstract class PoderMutante {

    private int dano;

    protected PoderMutante(int danoInicial) {
        this.dano = danoInicial;
    }

    public final void aplicarEfecto(Mutante atacante, Mutante objetivo) {
        aplicarEfecto(atacante, objetivo, false);
    }

    // Final a propósito: garantiza que TODA subclase suba de nivel su poder
    // cuando el ataque conecta, sin depender de que cada una se acuerde de hacerlo.
    public final void aplicarEfecto(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        int energiaAntes = objetivo.getEnergia();

        efectoEspecifico(atacante, objetivo, objetivoSeDefiende);

        if (objetivo.getEnergia() < energiaAntes) {
            aumentarDano();
        }
    }

    protected abstract void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende);

    public abstract TipoEfecto getTipoEfecto();

    private synchronized void aumentarDano() {
        if (dano < ConstantesJuego.DANO_MAX_PODER) {
            dano++;
        }
    }

    public synchronized int getDano() {
        return dano;
    }

    protected int danoConDefensa(Mutante objetivo, boolean objetivoSeDefiende){
        if (objetivoSeDefiende){
            return Math.max(1, getDano() / objetivo.getDefensa());
            // Math.max(1, ...) es un método de la propia API de Java: devuelve el mayor entre los dos valores que le pasas. 
            // Si la división da 0 (o técnicamente nunca menos), forzamos que el resultado nunca sea menor que 1. 
            // Sigue siendo cierto que defenderse reduce el daño (de 3 a 1, por ejemplo, en vez de a 0) 
            //  solo evita que "defenderse" se vuelva accidentalmente en "inmunidad total" por culpa de cómo Java redondea enteros.
        }
        return getDano();
    }
    //garantizar un mínimo de 1 punto de daño cuando el ataque conecta.
    //Esto es una decisión de diseño: un ataque que conecta (el defensor se defendió, pero el ataque igual "pasó") debería doler algo, aunque sea mínimo — la defensa reduce el daño, no lo debería anular por completo.
}