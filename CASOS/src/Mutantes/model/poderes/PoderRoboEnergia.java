package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderRoboEnergia extends PoderMutante {

    public PoderRoboEnergia(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.ROBO_ENERGIA;
    }

    @Override
    protected void efectoEspecifico(Mutante poseedor, Mutante objetivo) {
        objetivo.recibirDano(getDano());
        poseedor.recuperarEnergia(getDano() / 2);
    }
}