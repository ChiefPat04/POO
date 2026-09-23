package mutantes.model;

import mutantes.model.poderes.PoderEscudoEnergia;
import mutantes.model.poderes.PoderFuerza;
import mutantes.model.poderes.PoderInvisibilidad;
import mutantes.model.poderes.PoderRegeneracion;
import mutantes.model.poderes.PoderRoboEnergia;
import mutantes.model.poderes.PoderTeletransportacion;
import mutantes.model.poderes.PoderVelocidad;

public class ModelMain {

    public static void main(String[] args) {

        System.out.println("--- Prueba basica de energia y vida ---");

        Mutante mutante = new Mutante(1, "Wolverine", 2, new PoderFuerza(3));

        System.out.println("Energia inicial: " + mutante.getEnergia());
        System.out.println("Esta vivo: " + mutante.estaVivo());

        mutante.recibirDano(40);
        System.out.println("Energia despues de 40 de dano: " + mutante.getEnergia());

        mutante.recibirDano(80);
        System.out.println("Energia despues de otros 80 de dano: " + mutante.getEnergia());
        System.out.println("Esta vivo: " + mutante.estaVivo());

        mutante.recibirDano(50);
        System.out.println("Energia intentando danar a un muerto: " + mutante.getEnergia());

        System.out.println();
        System.out.println("--- 1) PoderFuerza: debe danar y SI subir de nivel ---");

        Mutante atacanteFuerza = new Mutante(2, "Atacante Fuerza", 1, new PoderFuerza(3));
        Mutante victimaFuerza = new Mutante(3, "Victima Fuerza", 1, null);

        atacanteFuerza.atacar(victimaFuerza);
        System.out.println("Energia de la victima (deberia bajar de 100 a 97): " + victimaFuerza.getEnergia());
        System.out.println("Nivel del poder (deberia subir de 3 a 4): " + atacanteFuerza.getPoder().getDano());

        System.out.println();
        System.out.println("--- 2) PoderVelocidad: debe danar y SI subir de nivel ---");

        Mutante atacanteVelocidad = new Mutante(4, "Atacante Velocidad", 1, new PoderVelocidad(2));
        Mutante victimaVelocidad = new Mutante(5, "Victima Velocidad", 1, null);

        atacanteVelocidad.atacar(victimaVelocidad);
        System.out.println("Energia de la victima (deberia bajar de 100 a 98): " + victimaVelocidad.getEnergia());
        System.out.println("Nivel del poder (deberia subir de 2 a 3): " + atacanteVelocidad.getPoder().getDano());

        System.out.println();
        System.out.println("--- 3) PoderRegeneracion: NO danar a nadie, cura al dueno, NO sube de nivel ---");

        Mutante sanador = new Mutante(6, "Sanador", 1, new PoderRegeneracion(4));
        Mutante rivalDeSanador = new Mutante(7, "Rival del sanador", 1, null);

        sanador.recibirDano(30);
        System.out.println("Energia del sanador tras recibir 30 de dano: " + sanador.getEnergia());

        sanador.atacar(rivalDeSanador);
        System.out.println("Energia del rival (NO deberia cambiar, sigue en 100): " + rivalDeSanador.getEnergia());
        System.out.println("Energia del sanador (deberia subir de 70 a 74): " + sanador.getEnergia());
        System.out.println("Nivel del poder (NO deberia subir, se queda en 4): " + sanador.getPoder().getDano());

        System.out.println();
        System.out.println("--- 4) PoderEscudoEnergia: activa el escudo, el SIGUIENTE golpe se reduce a la mitad ---");

        Mutante defensor = new Mutante(8, "Defensor con escudo", 1, new PoderEscudoEnergia(0));
        Mutante rivalDelDefensor = new Mutante(9, "Rival del defensor", 1, null);

        defensor.atacar(rivalDelDefensor);
        System.out.println("Rival del defensor (NO deberia cambiar, el escudo no ataca): " + rivalDelDefensor.getEnergia());

        defensor.recibirDano(20);
        System.out.println("Defensor tras recibir 20 CON escudo activo (deberia bajar solo 10, quedando en 90): " + defensor.getEnergia());

        defensor.recibirDano(20);
        System.out.println("Defensor tras un SEGUNDO golpe de 20, ya SIN escudo (deberia bajar completo, quedando en 70): " + defensor.getEnergia());

        System.out.println();
        System.out.println("--- 5) PoderInvisibilidad: activa invisibilidad, el SIGUIENTE golpe no hace nada ---");

        Mutante invisible = new Mutante(10, "Mutante invisible", 1, new PoderInvisibilidad(0));
        Mutante rivalDelInvisible = new Mutante(11, "Rival del invisible", 1, null);

        invisible.atacar(rivalDelInvisible);
        System.out.println("Rival del invisible (NO deberia cambiar): " + rivalDelInvisible.getEnergia());

        invisible.recibirDano(50);
        System.out.println("Invisible tras recibir 50 ESTANDO invisible (NO deberia cambiar, sigue en 100): " + invisible.getEnergia());

        invisible.recibirDano(50);
        System.out.println("Invisible tras un SEGUNDO golpe, ya visible (deberia bajar completo, quedando en 50): " + invisible.getEnergia());

        System.out.println();
        System.out.println("--- 6) PoderRoboEnergia: dana al enemigo Y cura al dueno, SI sube de nivel ---");

        Mutante ladron = new Mutante(12, "Ladron de energia", 1, new PoderRoboEnergia(4));
        Mutante victimaLadron = new Mutante(13, "Victima del ladron", 1, null);

        ladron.recibirDano(20);
        System.out.println("Ladron tras recibir 20 de dano (queda en 80): " + ladron.getEnergia());

        ladron.atacar(victimaLadron);
        System.out.println("Energia de la victima (deberia bajar de 100 a 96): " + victimaLadron.getEnergia());
        System.out.println("Energia del ladron (deberia subir de 80 a 82, se curo la mitad del dano): " + ladron.getEnergia());
        System.out.println("Nivel del poder (deberia subir de 4 a 5): " + ladron.getPoder().getDano());

        System.out.println();
        System.out.println("--- 7) PoderTeletransportacion: no hace nada todavia (se implementa en la capa Control) ---");

        Mutante teletransportador = new Mutante(14, "Teletransportador", 1, new PoderTeletransportacion(0));
        Mutante rivalDelTeletransportador = new Mutante(15, "Rival del teletransportador", 1, null);

        teletransportador.atacar(rivalDelTeletransportador);
        System.out.println("Rival (NO deberia cambiar): " + rivalDelTeletransportador.getEnergia());
        System.out.println("Teletransportador (NO deberia cambiar): " + teletransportador.getEnergia());
    }
}

// Con la salida de estos datos confirmamos que: el poder hace daño real al enemigo, cura de verdad al dueño (algo que antes no pasaba),
//  y el nivel del poder sube automáticamente solo cuando el golpe fue efectivo — las tres reglas del enunciado funcionando juntas, 
// sin que ninguna clase tenga que "acordarse" manualmente de aplicarlas todas.