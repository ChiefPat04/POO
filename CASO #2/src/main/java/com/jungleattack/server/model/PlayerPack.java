package com.jungleattack.server.model;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * El pack de un jugador: 10 animales que empiezan con daño 2.
 */
public class PlayerPack extends Pack {

    private final Player owner;

    public PlayerPack(Player owner, Species species) {
        super(species, GameConstants.PLAYER_START_DAMAGE);
        this.owner = owner;
    }

    //LA FÁBRICA
    @Override
    protected Animal createAnimal(int id, Position position) {
        return new PlayerAnimal(id, this, position);
    }

    @Override
    public int getSize() {
        return GameConstants.PLAYER_PACK_SIZE;
    }

    @Override
    public boolean isBot() {
        return false;
    }

    // Le suma energía a cada animal vivo, sin pasar de MAX_PLAYER_ENERGY. 
    public void healAll(int amount) {
        for (Animal a : getAliveMembers()) {
            a.heal(amount, GameConstants.MAX_PLAYER_ENERGY);
        }
    }

    public Player getOwner() {
        return owner;
    }
}
