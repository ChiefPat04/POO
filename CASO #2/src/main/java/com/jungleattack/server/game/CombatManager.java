package com.jungleattack.server.game;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.server.model.Animal;
import com.jungleattack.server.model.PlayerPack;
import com.jungleattack.server.util.GameConstants;

/**
 * Los ataques de la partida.
 * Está separada de GameSession para poder probarla sola.
 */
public class CombatManager {

    /**
     * Hace que cada animal vivo ataque si puede.
     *
     * @return true si hubo algún ataque en esta llamada
     */
    public boolean processAttacks(List<Animal> all, long now) {
        boolean anyAttack = false;
        for (Animal attacker : all) {
            if (!attacker.isAlive()) {
                continue;
            }
            List<Animal> enemies = findEnemiesInRange(attacker, all);
            int energyBefore = totalEnergy(enemies);

            Animal killed = attacker.tryAttack(enemies, now);

            // Si la energía de los enemigos cambió, alguien recibió daño
            if (totalEnergy(enemies) != energyBefore) {
                anyAttack = true;
            }
            if (killed != null) {
                registerKill(attacker, killed);
            }
        }
        return anyAttack;
    }

    /** Los animales vivos, de otro pack, que están dentro del rango del atacante. */
    public List<Animal> findEnemiesInRange(Animal attacker, List<Animal> all) {
        List<Animal> enemies = new ArrayList<>();
        for (Animal other : all) {
            if (other.isAlive() && attacker.isEnemyOf(other) && attacker.isInRange(other)) {
                enemies.add(other);
            }
        }
        return enemies;
    }

    /** Si un animal de jugador mató a un bot, cuenta la baja y da el bono cada 2 bots. */
    private void registerKill(Animal attacker, Animal victim) {
        if (attacker.isBot() || !victim.isBot()) {
            return;
        }
        PlayerPack pack = (PlayerPack) attacker.getPack();
        if (pack.getOwner().addBotKill()) {
            pack.healAll(GameConstants.ENERGY_BONUS);
        }
    }

    private int totalEnergy(List<Animal> animals) {
        int total = 0;
        for (Animal a : animals) {
            total += a.getEnergy();
        }
        return total;
    }
}
