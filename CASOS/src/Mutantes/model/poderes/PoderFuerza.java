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
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        objetivo.recibirDano(danoConDefensa(objetivo, objetivoSeDefiende));
    }
}