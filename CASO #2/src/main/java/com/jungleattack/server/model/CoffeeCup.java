package com.jungleattack.server.model;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Una taza de café. El primer pack que la toca gana +1 de daño.
 */
public class CoffeeCup extends MapItem {

    private boolean taken;

    public CoffeeCup(int id, Position position) {
        super(id, position, GameConstants.PICKUP_RADIUS, GameConstants.COFFEE_IMAGE);
        this.taken = false;
    }

    public boolean isTaken() {
        return taken;
    }

    public void take() {
        taken = true;
    }
}
