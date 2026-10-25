package com.jungleattack.server.util;

/**
 * Un punto (x, y) del mapa.
 * Es mutable: cuando un animal camina, su posición cambia.
 * Cuidado: dos objetos no deben compartir la misma Position, por eso existe el constructor de copia.
 */
public class Position {

    private double x;
    private double y;

    public Position(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Crea una copia nueva con los mismos valores que "other".
    public Position(Position other) {
        this.x = other.x;
        this.y = other.y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    // Distancia en línea recta (teorema de Pitágoras).
    public double distanceTo(Position other) {
        double dx = other.x - x;
        double dy = other.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Avanza "distance" píxeles hacia "target".
     * Si el destino está más cerca que eso, se queda justo en el destino (no se pasa).
     */
    public void moveTowards(Position target, double distance) {
        double remaining = distanceTo(target);
        if (remaining <= distance) {
            x = target.x;
            y = target.y;
            return;
        }
        // Avanzamos una fracción del camino: distance / remaining
        double fraction = distance / remaining;
        x = x + (target.x - x) * fraction;
        y = y + (target.y - y) * fraction;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
