package mutantes.model.poderes;

import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;

public abstract class PoderMutante {

    private int dano;

    protected PoderMutante(int danoInicial) {
        this.dano = danoInicial;
    }

    public synchronized void aumentarDano() {
        if (dano < ConstantesJuego.DANO_MAX_PODER) {
            dano++;
        }
    }

    public synchronized int getDano() {
        return dano;
    }

    public abstract TipoEfecto getTipoEfecto();

    public abstract void aplicarEfecto(Mutante objetivo);
}