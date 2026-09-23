package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderInvisibilidad extends PoderMutante {

    public PoderInvisibilidad(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.INVISIBILIDAD;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        atacante.activarInvisibilidad();
    }
}