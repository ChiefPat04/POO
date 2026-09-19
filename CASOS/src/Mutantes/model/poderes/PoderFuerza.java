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
    public void efectoEspecifico(Mutante atacante, Mutante objetivo) { // No pasa nada porque un parámetro no se use dentro del método; Java lo permite sin problema, y mantener la misma firma en las tres subclases es justamente lo que hace que el polimorfismo funcione.
        objetivo.recibirDano(getDano());
    }
}
