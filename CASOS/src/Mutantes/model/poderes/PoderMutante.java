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
            return getDano() / objetivo.getDefensa();
        }
        return getDano();
    }
    //Es exactamente la fórmula del enunciado (daño completo, o daño dividido entre defensa si el objetivo se defiende), 
    // extraída una sola vez aquí en la clase padre, 
    // para que las subclases que sí hacen daño directo (PoderFuerza, PoderVelocidad, PoderRoboEnergia) no tengan que repetir el mismo if cada una por su cuenta. 
    // Es protected porque solo las subclases la necesitan, nadie más afuera.
}