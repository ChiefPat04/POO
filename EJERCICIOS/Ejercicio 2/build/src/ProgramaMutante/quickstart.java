package programaMutante;

import personas.Persona;
import poderes.IPower;
import poderes.PoderFuego;
import poderes.PoderFuerza;
import poderes.PoderTelepatía;
import poderes.PoderTormenta;
import poderes.PoderVelocidad;
import profesiones.IAEngineer;
import profesiones.Mecánico;
import profesiones.Trader;

public class quickstart {
    public static void main(String[] args) { 
        Persona[] profesionales = new Persona[10];
        IPower[] poderesDisponibles = {
            new PoderFuego(), new PoderFuerza(), new PoderTelepatía(),
            new PoderTormenta(), new PoderVelocidad()
        };

        for (int i = 0; i < profesionales.length; i++) {
            int tipoProfesion = i < 3 ? i : (int) (Math.random() * 3);

            switch (tipoProfesion) {
                case 0:
                    profesionales[i] = new IAEngineer("Gary Kildall " + i, (byte) (25 + i), "robotica");
                    break;
                case 1:
                    profesionales[i] = new Mecánico("Don Luis " + i, (byte) (30 + i), "motores electricos");
                    break;
                case 2:
                    profesionales[i] = new Trader("Trader Gurú " + i, (byte) (28 + i), "tecnologia");
                    break;
                default:
                    throw new IllegalStateException("Tipo de profesion inesperado");
            }

                int tipoPoder = i < poderesDisponibles.length
                    ? i
                    : (int) (Math.random() * poderesDisponibles.length);
            profesionales[i].setPower(poderesDisponibles[tipoPoder]);
        }

        for (Persona persona : profesionales) {
            persona.trabajar();
            System.out.println("Ataca " + persona.getNombre());
            persona.atacar();
        }

        System.out.println("Personas creadas: " + Persona.getTotalPersonas());
    }
}
