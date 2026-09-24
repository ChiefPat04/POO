package mutantes.model.poderes;

import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;

public class PoderVeneno extends PoderMutante {

    public PoderVeneno(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.VENENO;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        int danoPorPorcentaje = (int) (objetivo.getEnergia() * ConstantesJuego.PORCENTAJE_DANO_VENENO);
        int danoFinal = Math.max(getDano(), danoPorPorcentaje);
        objetivo.recibirDano(danoFinal);
    }
}