package mutantes.model.poderes;

import mutantes.constants.ConstantesJuego;
import mutantes.model.Mutante;

public class PoderLlamarada extends PoderMutante {

    public PoderLlamarada(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.LLAMARADA;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        int danoBase = danoConDefensa(objetivo, objetivoSeDefiende);
        objetivo.recibirDano(danoBase * ConstantesJuego.MULTIPLICADOR_LLAMARADA);
    }
}
