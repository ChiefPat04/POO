package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderRegeneracion extends PoderMutante {

    public PoderRegeneracion(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.REGENERACION;
    }

    @Override
    public void aplicarEfecto(Mutante objetivo) {
        objetivo.recibirDano(getDano() / 2);
    }
}