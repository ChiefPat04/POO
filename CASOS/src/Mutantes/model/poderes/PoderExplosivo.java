package mutantes.model.poderes;

import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;

public class PoderExplosivo extends PoderMutante {

    public PoderExplosivo(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.EXPLOSION;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        // Explota ignorando la defensa, y ademas duplica el dano base.
        int danoFinal = getDano() * ConstantesJuego.MULTIPLICADOR_EXPLOSIVO;
        objetivo.recibirDano(danoFinal);
    }
}