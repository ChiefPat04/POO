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
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo) {
        objetivo.recibirDano(getDano());
    }
}

//NOTA:por ahora PoderFuerza y PoderVelocidad hacen exactamente lo mismo en combate, está bien así. La diferencia real de "Velocidad" (afectar el radio de detección o la frecuencia de movimiento) va a vivir en la capa control, más adelante, no aquí. No hace falta inventarle una diferencia falsa en este punto.