package com.jungleattack.server.model;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Lo que tienen en común los animales del jugador y los bots.
 * Es abstracta: nunca se hace "new Animal(...)", sino new PlayerAnimal o new BotAnimal.
 */
public abstract class Animal {

    private final int id;
    private final Pack pack;
    private final Position position;
    private int energy;
    private long lastAttackTime;
    private final List<Position> route;

    protected Animal(int id, Pack pack, Position position, int energy) {
        this.id = id;
        this.pack = pack;
        this.position = new Position(position);
        this.energy = energy;
        this.lastAttackTime = -GameConstants.ATTACK_COOLDOWN_MS;
        this.route = new ArrayList<>();
    }

    // Energía

    public boolean isAlive() {
        return energy > 0;
    }

    public void receiveDamage(int amount) {
        energy = energy - amount;
        if (energy < 0) {
            energy = 0;
        }
    }

    public void heal(int amount, int max) {
        if (!isAlive()) {
            return;
        }
        energy = energy + amount;
        if (energy > max) {
            energy = max;
        }
    }

    // Combate

    public boolean canAttack(long now) {
        return now - lastAttackTime >= GameConstants.ATTACK_COOLDOWN_MS;
    }

    public boolean isInRange(Animal other) {
        return position.distanceTo(other.position) <= GameConstants.ENCOUNTER_RADIUS;
    }

    public boolean isEnemyOf(Animal other) {
        if (pack == other.pack) {
            return false;
        }
        if (isBot() && other.isBot()) {
            return false;
        }
        return true;
    }

    /**
     * Patrón Template Method: los pasos de un ataque son siempre los mismos.
     * Lo único que cambia entre hijos es chooseTarget().
     * Es "final" para que ningún hijo pueda cambiar estos pasos.
     *
     * @return el animal que murió con este ataque, o null si nadie murió
     */
    public final Animal tryAttack(List<Animal> enemiesInRange, long now) {
        if (!isAlive() || !canAttack(now)) {
            return null;
        }
        Animal target = chooseTarget(enemiesInRange);
        if (target == null) {
            return null;
        }
        target.receiveDamage(pack.getDamage());
        lastAttackTime = now;
        if (!target.isAlive()) {
            return target;
        }
        return null;
    }

    protected abstract Animal chooseTarget(List<Animal> enemiesInRange);

    // Movimiento

    public void setRoute(List<Position> newRoute) {
        route.clear();
        for (Position p : newRoute) {
            route.add(new Position(p));
        }
    }

    public void stopMoving() {
        route.clear();
    }

    public boolean isMoving() {
        return !route.isEmpty();
    }

    /**
     * Algoritmo
     * Avanza getSpeed() * seconds píxeles siguiendo la ruta.
     * Si llega a un punto y le sobra distancia, sigue hacia el siguiente.
     * El cliente usa este mismo cálculo.
     */
    public void moveStep(double seconds) {
        double distance = getSpeed() * seconds;
        while (distance > 0 && !route.isEmpty()) {
            Position next = route.get(0);
            double d = position.distanceTo(next);
            if (d <= distance) {
                position.setX(next.getX());
                position.setY(next.getY());
                route.remove(0);
                distance = distance - d;
            } else {
                position.moveTowards(next, distance);
                distance = 0;
            }
        }
    }

    public abstract double getSpeed();
    public abstract boolean isBot();

    // Getters

    public int getId() { 
        return id;
    }

    public Pack getPack() {
        return pack;
    }

    public Species getSpecies() {
        return pack.getSpecies();
    }

    public Position getPosition() { 
        return position; 
    }

    public int getEnergy() {
        return energy; 
    }

    public List<Position> getRoute() { 
        List<Position> copy = new ArrayList<>(); 
        for (Position p : route) { 
            copy.add(new Position(p));
        }
        return copy; 
    }
}
