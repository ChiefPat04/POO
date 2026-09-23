package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderTeletransportacion extends PoderMutante {

    public PoderTeletransportacion(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.TELETRANSPORTACION;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        // Vacio a proposito: mover al mutante requiere conocer las
        // dimensiones del CampoDeBatalla, que pertenece a la capa Game.
        // La capa Control sera quien dispare el movimiento real.
    }
}