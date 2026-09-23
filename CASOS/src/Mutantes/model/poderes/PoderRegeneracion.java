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
    protected void efectoEspecifico(Mutante poseedor, Mutante objetivo) {
        poseedor.recuperarEnergia(getDano());
    }
}

//Ahora sí cumple su nombre: le hace daño normal al enemigo (getDano(), igual que los otros) y además cura a su propio dueño la mitad de ese valor. 
// Esto es una diferencia de comportamiento real, no solo un número distinto — ahora si alguien pregunta "¿por qué existen 3 subclases y no 1 sola con un parámetro?", 
// la respuesta es clara: porque cada una hace algo cualitativamente distinto, y eso es exactamente lo que el polimorfismo está para resolver.