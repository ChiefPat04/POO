package com.jungleattack.server.model;

import com.jungleattack.server.util.Position;

/**
 * Algo que está quieto en el mapa y que un animal puede tocar (café o bandera).
 */
public abstract class MapItem {

    private final int id;
    private final Position position;
    private final double touchRadius;
    private final String imageFile;

    protected MapItem(int id, Position position, double touchRadius, String imageFile) {
        this.id = id;
        this.position = new Position(position);
        this.touchRadius = touchRadius;
        this.imageFile = imageFile;
    }

    /** Un animal vivo lo toca si está a "touchRadius" píxeles o menos. */
    public boolean isTouchedBy(Animal animal) {
        if (!animal.isAlive()) {
            return false;
        }
        return animal.getPosition().distanceTo(position) <= touchRadius;
    }

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public double getTouchRadius() {
        return touchRadius;
    }

    public String getImageFile() {
        return imageFile;
    }
}
