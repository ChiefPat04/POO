package com.jungleattack.server.map;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.server.util.Position;

/**
 * Un punto del grafo de caminos (el centro de una zona o una de sus puertas).
 */
public class PathNode {

    private final int id;
    private final Position position;
    private final int zoneId;
    private final List<PathNode> neighbors;

    public PathNode(int id, Position position, int zoneId) {
        this.id = id;
        this.position = new Position(position);
        this.zoneId = zoneId;
        this.neighbors = new ArrayList<>();
    }

    // Une los dos nodos en los dos sentidos (el grafo no tiene dirección). 
    public void connect(PathNode other) {
        if (!neighbors.contains(other)) {
            neighbors.add(other);
        }
        if (!other.neighbors.contains(this)) {
            other.neighbors.add(this);
        }
    }

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public int getZoneId() {
        return zoneId;
    }

    public List<PathNode> getNeighbors() {
        return neighbors;
    }
}
