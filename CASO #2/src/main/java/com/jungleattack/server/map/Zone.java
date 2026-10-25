package com.jungleattack.server.map;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Una de las 12 zonas del mapa. Mide lo mismo que la pantalla del cliente (800 x 600).
 */
public class Zone {

    private final int id;
    private final int column;
    private final int row;
    private final PathNode center;
    private final List<PathNode> nodes;

    public Zone(int id, int column, int row, PathNode center) {
        this.id = id;
        this.column = column;
        this.row = row;
        this.center = center;
        this.nodes = new ArrayList<>();
        this.nodes.add(center);
    }

    // Agrega una puerta a la zona. Lo usa GameMap al armar el mapa.
    public void addNode(PathNode node) {
        nodes.add(node);
    }

    // La x del borde izquierdo de la zona.
    public double getMinX() {
        return column * GameConstants.ZONE_WIDTH;
    }

    // La y del borde de arriba de la zona.
    public double getMinY() {
        return row * GameConstants.ZONE_HEIGHT;
    }

    // true si el punto está dentro de esta zona.
    public boolean contains(Position p) {
        return p.getX() >= getMinX() && p.getX() < getMinX() + GameConstants.ZONE_WIDTH
                && p.getY() >= getMinY() && p.getY() < getMinY() + GameConstants.ZONE_HEIGHT;
    }

    public int getId() {
        return id;
    }

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public PathNode getCenter() {
        return center;
    }

    public List<PathNode> getNodes() {
        return nodes;
    }
}
