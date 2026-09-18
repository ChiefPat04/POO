package mutantes.model.poderes;
import mutantes.model.Mutante;

public class PoderVelocidad extends PoderMutante {

    public PoderVelocidad(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.RAYO;
    }

    @Override
    public void aplicarEfecto(Mutante objetivo) {
        objetivo.recibirDano(getDano());
    }
}