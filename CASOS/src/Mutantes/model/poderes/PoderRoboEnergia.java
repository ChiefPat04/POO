package mutantes.model.poderes;

import mutantes.model.Mutante;

public class PoderRoboEnergia extends PoderMutante {

    public PoderRoboEnergia(int danoInicial) {
        super(danoInicial);
    }

    @Override
    public TipoEfecto getTipoEfecto() {
        return TipoEfecto.ROBO_ENERGIA;
    }

    @Override
    protected void efectoEspecifico(Mutante atacante, Mutante objetivo, boolean objetivoSeDefiende) {
        int danoFinal = danoConDefensa(objetivo, objetivoSeDefiende);
        objetivo.recibirDano(danoFinal);
        atacante.recuperarEnergia(getDano() / 2);
    }
}

//le corregí el parámetro poseedor a atacante para que coincida con el nombre que usa PoderMutante en la firma del método abstracto 
//mismo objeto, solo homogenizamos el nombre entre todas las subclases para que sea consistente leerlas.