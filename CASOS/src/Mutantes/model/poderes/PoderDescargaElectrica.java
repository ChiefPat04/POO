package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderDescargaElectrica extends PoderMutante {

    public PoderDescargaElectrica(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.DESCARGA_ELECTRICA;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        // El rayo ignora la defensa: siempre aplica el dano completo del poder.
        objetivo.recibirDano(getDano());
    }
}