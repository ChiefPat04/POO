package com.jungleattack.server.model;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * La bandera de un jugador. Si un animal rival la toca, ese rival gana.
 */
public class Flag extends MapItem {

    private final Player owner;

    /**
     * @param playerNumber 1 o 2. El jugador 1 tiene bandera roja y el 2 azul.
     */
    public Flag(int id, Position position, Player owner, int playerNumber) {
        super(id, position, GameConstants.FLAG_RADIUS,
                playerNumber == 1 ? GameConstants.FLAG_IMAGE_PLAYER1 : GameConstants.FLAG_IMAGE_PLAYER2);
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }
}
