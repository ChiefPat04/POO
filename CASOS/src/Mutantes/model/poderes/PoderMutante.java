package mutantes.model.poderes;

import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;

public abstract class PoderMutante {

    private int dano;

    protected PoderMutante(int danoInicial) {
        this.dano = danoInicial;
    }

    public final void aplicarEfecto(Mutante atacante, Mutante objetivo){
        int energiaAntes = objetivo.getEnergia();

        efectoEspecifico(atacante, objetivo);

        if (objetivo.getEnergia() < energiaAntes) {
            aumentarDano();
        }
    }

    protected abstract void efectoEspecifico(Mutante atacante, Mutante objetivo);

    public abstract TipoEfecto getTipoEfecto();

    public synchronized void aumentarDano() {
        if (dano < ConstantesJuego.DANO_MAX_PODER) {
            dano++;
        }
    }

    public synchronized int getDano() {
        return dano;
    }
}