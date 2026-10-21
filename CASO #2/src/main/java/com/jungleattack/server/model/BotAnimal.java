package com.jungleattack.server.model;

import java.util.List;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Un animal de un pack bot.
 * Ataca al enemigo vivo más cercano.
 */
public class BotAnimal extends Animal {

    public BotAnimal(int id, Pack pack, Position position) {
        super(id, pack, position, GameConstants.BOT_START_ENERGY);
    }

    @Override
    protected Animal chooseTarget(List<Animal> enemiesInRange) {
        Animal closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Animal enemy : enemiesInRange) {
            if (!enemy.isAlive()) {
                continue;
            }
            double d = getPosition().distanceTo(enemy.getPosition());
            if (d < closestDistance) {
                closest = enemy;
                closestDistance = d;
            }
        }
        return closest;
    }

    @Override
    public double getSpeed() {
        return GameConstants.BOT_SPEED;
    }

    @Override
    public boolean isBot() {
        return true;
    }
}
