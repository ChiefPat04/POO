package mutantes.control;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import mutantes.game.CampoDeBatalla;
import mutantes.model.AccionCombate;
import mutantes.model.Mutante;
import mutantes.model.poderes.TipoEfecto;

public class GestorCombate {

    private final CampoDeBatalla campo;
    // Set concurrente: evita que el mismo par de mutantes se resuelva dos veces
    // si ambos hilos detectan el encuentro casi al mismo tiempo. add() devuelve
    // false si la clave ya existia, asi que sirve como "candado" y registro a la vez.
    private final Set<String> paresEnResolucion = ConcurrentHashMap.newKeySet();

    public GestorCombate(CampoDeBatalla campo) {
        this.campo = campo;
    }

    public void resolverEncuentro(Mutante m1, Mutante m2){
        String clave = claveParaPar(m1,m2);

        if (!paresEnResolucion.add(clave)){
            return;
        }

        try {
            if (!m1.estaVivo() || !m2.estaVivo()) {
                return;
            }

            AccionCombate accion1 = m1.decidirAccion();
            AccionCombate accion2 = m2.decidirAccion();

            if (accion1 == AccionCombate.ATACAR){
                m1.atacar(m2, accion2 == AccionCombate.DEFENDER);
                reposicionarSiTeletransportador(m1);
            }

            if (accion2 == AccionCombate.ATACAR){
                m2.atacar(m1, accion1 == AccionCombate.DEFENDER);
                reposicionarSiTeletransportador(m2);
            }
        } finally {
            // Siempre libera la clave, incluso si algo falla, para que este par
            // pueda volver a combatir en un encuentro futuro.
            paresEnResolucion.remove(clave);
        }
    }

    private void reposicionarSiTeletransportador(Mutante mutante){
        if (mutante.getPoder() == null){
            return;
        }

        if (mutante.getPoder().getTipoEfecto() != TipoEfecto.TELETRANSPORTACION){
            return;
        }

        int [] dimensiones = campo.getDimensiones();
        int nuevaX = ThreadLocalRandom.current().nextInt(dimensiones[0] + 1);
        int nuevaY = ThreadLocalRandom.current().nextInt(dimensiones[1] + 1);

        mutante.reposicionar(nuevaX, nuevaY);
    }

    private String claveParaPar(Mutante m1, Mutante m2){
        int idMenor = Math.min(m1.getId(), m2.getId());
        int idMayor = Math.max(m1.getId(), m2.getId());
        return idMenor + "-" + idMayor;
    }
}





































