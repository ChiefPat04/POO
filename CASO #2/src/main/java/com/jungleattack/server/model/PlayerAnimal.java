package com.jungleattack.server.model;

import java.util.List;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Un animal del pack de un jugador.
 * Solo ataca al animal que el jugador le ordenó atacar.
 */
public class PlayerAnimal extends Animal {

    private Animal orderedTarget;

    public PlayerAnimal(int id, Pack pack, Position position) {
        super(id, pack, position, GameConstants.PLAYER_START_ENERGY);
    }

    public Animal getOrderedTarget() {
        return orderedTarget;
    }

    public void setOrderedTarget(Animal target) {
        this.orderedTarget = target;
    }

    /**
     * Devuelve solo el objetivo ordenado.
     * Si murió o se salió del rango, lo olvida y devuelve null.
     */
    @Override
    protected Animal chooseTarget(List<Animal> enemiesInRange) {
        if (orderedTarget == null) {
            return null;
        }
        if (!orderedTarget.isAlive() || !isInRange(orderedTarget)) {
            orderedTarget = null;
            return null;
        }
        return orderedTarget;
    }

    @Override
    public double getSpeed() {
        return GameConstants.ANIMAL_SPEED;
    }

    @Override
    public boolean isBot() {
        return false;
    }
}
