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
         // NOTA: esta formula usa la energia ACTUAL del objetivo, por lo que en
         // realidad hace mas dano contra rivales con energia alta, no contra los
         // debilitados (al reves de lo que sugiere la descripcion del poder).
        int danoPorPorcentaje = (int) (objetivo.getEnergia() * ConstantesJuego.PORCENTAJE_DANO_VENENO);
        int danoFinal = Math.max(getDano(), danoPorPorcentaje);
        objetivo.recibirDano(danoFinal);
    }
}