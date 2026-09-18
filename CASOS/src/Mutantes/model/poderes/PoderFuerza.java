package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderFuerza extends PoderMutante {

    public PoderFuerza(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.FUEGO;
    }

    @Override
    public void aplicarEfecto(Mutante objetivo) {
        objetivo.recibirDano(getDano());
    }
}
