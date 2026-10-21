package com.jungleattack.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Una manada de animales de la misma especie.
 * El daño es del pack: todos sus miembros lo comparten.
 */
public abstract class Pack {

    private final Species species;
    private final List<Animal> members;
    private int damage;

    protected Pack(Species species, int damage) {
        this.species = species;
        this.damage = damage;
        this.members = new ArrayList<>();
    }

    /**
     * Crea getSize() animales repartidos alrededor de "center".
     * Factory Method: no pregunta con "if" qué tipo crear, eso lo decide createAnimal().
     */
    public void createMembers(Position center, int firstId, Random random) {
        for (int i = 0; i < getSize(); i++) {
            double offsetX = (random.nextDouble() * 2 - 1) * GameConstants.SPAWN_SPREAD;
            double offsetY = (random.nextDouble() * 2 - 1) * GameConstants.SPAWN_SPREAD;
            Position p = new Position(center.getX() + offsetX, center.getY() + offsetY);
            members.add(createAnimal(firstId + i, p));
        }
    }

    /** Cada hijo crea su propio tipo de animal (PlayerAnimal o BotAnimal). */
    protected abstract Animal createAnimal(int id, Position position);

    public abstract int getSize();

    public abstract boolean isBot();

    public void increaseDamage(int amount) {
        damage = damage + amount;
    }

    public List<Animal> getAliveMembers() {
        List<Animal> alive = new ArrayList<>();
        for (Animal a : members) {
            if (a.isAlive()) {
                alive.add(a);
            }
        }
        return alive;
    }

    public int countDead() {
        int dead = 0;
        for (Animal a : members) {
            if (!a.isAlive()) {
                dead++;
            }
        }
        return dead;
    }

    /** true si todos sus animales murieron. */
    public boolean isEliminated() {
        return getAliveMembers().isEmpty();
    }

    /** Busca un animal del pack por su id. Devuelve null si no está. */
    public Animal findAnimal(int id) {
        for (Animal a : members) {
            if (a.getId() == id) {
                return a;
            }
        }
        return null;
    }

    public Species getSpecies() {
        return species;
    }

    public int getDamage() {
        return damage;
    }

    /** Una copia de la lista, para que nadie la cambie desde afuera. */
    public List<Animal> getMembers() {
        return new ArrayList<>(members);
    }
}
