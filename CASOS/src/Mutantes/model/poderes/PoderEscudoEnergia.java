package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderEscudoEnergia extends PoderMutante {

    public PoderEscudoEnergia(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.ESCUDO_ENERGIA;
    }

    @Override
    protected void efectoEspecifico(Mutante poseedor, Mutante objetivo) {
        poseedor.activarEscudo();
    }
}