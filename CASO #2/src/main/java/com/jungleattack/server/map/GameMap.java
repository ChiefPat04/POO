package com.jungleattack.server.map;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * El mapa completo: 12 zonas en una cuadrícula de 4 columnas x 3 filas.
 *
 * Cada zona tiene un nodo en el centro y una puerta hacia cada zona vecina.
 * El centro se une con sus puertas, y cada puerta con la puerta de la zona vecina.
 *
 * Los ids de las zonas van así:
 *   0  1  2  3
 *   4  5  6  7
 *   8  9 10 11
 */
public class GameMap {

    private final List<Zone> zones;
    private final List<PathNode> nodes;
    private int nextNodeId;

    public GameMap() {
        zones = new ArrayList<>();
        nodes = new ArrayList<>();
        nextNodeId = 0;
        createZones();
        connectNeighbors();
    }

    // Paso 1: crear las 12 zonas, cada una con su nodo del centro. 
    private void createZones() {
        for (int row = 0; row < GameConstants.ZONE_ROWS; row++) {
            for (int column = 0; column < GameConstants.ZONE_COLUMNS; column++) {
                int zoneId = row * GameConstants.ZONE_COLUMNS + column;
                double centerX = column * GameConstants.ZONE_WIDTH + GameConstants.ZONE_WIDTH / 2.0;
                double centerY = row * GameConstants.ZONE_HEIGHT + GameConstants.ZONE_HEIGHT / 2.0;
                PathNode center = createNode(centerX, centerY, zoneId);
                zones.add(new Zone(zoneId, column, row, center));
            }
        }
    }

    // Paso 2: poner las puertas entre cada par de zonas vecinas y unirlas. 
    private void connectNeighbors() {
        for (Zone zone : zones) {
            double minX = zone.getMinX();
            double minY = zone.getMinY();
            double centerX = zone.getCenter().getPosition().getX();
            double centerY = zone.getCenter().getPosition().getY();

            // Vecino de la derecha
            if (zone.getColumn() < GameConstants.ZONE_COLUMNS - 1) {
                Zone right = getZone(zone.getId() + 1);
                double borderX = minX + GameConstants.ZONE_WIDTH;
                PathNode myDoor = addDoor(zone, borderX - GameConstants.DOOR_MARGIN, centerY);
                PathNode otherDoor = addDoor(right, borderX + GameConstants.DOOR_MARGIN, centerY);
                myDoor.connect(otherDoor);
            }

            // Vecino de abajo
            if (zone.getRow() < GameConstants.ZONE_ROWS - 1) {
                Zone below = getZone(zone.getId() + GameConstants.ZONE_COLUMNS);
                double borderY = minY + GameConstants.ZONE_HEIGHT;
                PathNode myDoor = addDoor(zone, centerX, borderY - GameConstants.DOOR_MARGIN);
                PathNode otherDoor = addDoor(below, centerX, borderY + GameConstants.DOOR_MARGIN);
                myDoor.connect(otherDoor);
            }
        }
    }

    // Crea una puerta en la zona y la une con el centro de esa zona. 
    private PathNode addDoor(Zone zone, double x, double y) {
        PathNode door = createNode(x, y, zone.getId());
        zone.addNode(door);
        door.connect(zone.getCenter());
        return door;
    }

    private PathNode createNode(double x, double y, int zoneId) {
        PathNode node = new PathNode(nextNodeId, new Position(x, y), zoneId);
        nextNodeId++;
        nodes.add(node);
        return node;
    }

    // Métodos públicos 

    // Los ids coinciden con la posición en la lista, por eso basta con get(id). 
    public Zone getZone(int id) {
        return zones.get(id);
    }

    // columna = x / 800, fila = y / 600. Si el punto se sale del mapa, usa la zona del borde. 
    public int getZoneIdAt(Position p) {
        int column = (int) (p.getX() / GameConstants.ZONE_WIDTH);
        int row = (int) (p.getY() / GameConstants.ZONE_HEIGHT);
        column = Math.max(0, Math.min(column, GameConstants.ZONE_COLUMNS - 1));
        row = Math.max(0, Math.min(row, GameConstants.ZONE_ROWS - 1));
        return row * GameConstants.ZONE_COLUMNS + column;
    }

    // Recorre todos los nodos y devuelve el que está más cerca de p. 
    public PathNode findNearestNode(Position p) {
        PathNode nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (PathNode node : nodes) {
            double d = node.getPosition().distanceTo(p);
            if (d < nearestDistance) {
                nearest = node;
                nearestDistance = d;
            }
        }
        return nearest;
    }

    /**
     * El nodo al que van los animales cuando el jugador hace clic en "click".
     * Normalmente es el nodo más cercano. Pero si el clic cae cerca de una puerta, del lado del borde
     * (entre la puerta y la otra zona), el destino es la puerta de la zona de al lado.
     * Así el jugador puede cruzar de zona sin tener que ver la zona de al lado.
     */
    public PathNode findClickNode(Position click) {
        PathNode nearest = findNearestNode(click);
        if (click.distanceTo(nearest.getPosition()) > GameConstants.DOOR_CLICK_RADIUS) {
            return nearest;
        }
        for (PathNode neighbor : nearest.getNeighbors()) {
            // Una puerta tiene un vecino en otra zona: la puerta de la zona de al lado
            if (neighbor.getZoneId() != nearest.getZoneId() && isTowards(click, nearest, neighbor)) {
                return neighbor;
            }
        }
        return nearest;
    }

    // true si el clic está del lado de "to" respecto a "from" (producto punto mayor que 0). 
    private boolean isTowards(Position click, PathNode from, PathNode to) {
        double dx = to.getPosition().getX() - from.getPosition().getX();
        double dy = to.getPosition().getY() - from.getPosition().getY();
        double cx = click.getX() - from.getPosition().getX();
        double cy = click.getY() - from.getPosition().getY();
        return cx * dx + cy * dy > 0;
    }

    // Todos los nodos que no son centros (sirve para poner los cafés). 
    public List<PathNode> getDoorNodes() {
        List<PathNode> doors = new ArrayList<>();
        for (Zone zone : zones) {
            for (PathNode node : zone.getNodes()) {
                if (node != zone.getCenter()) {
                    doors.add(node);
                }
            }
        }
        return doors;
    }

    /**
     * Las líneas [x1, y1, x2, y2] de los caminos, para dibujarlas en el cliente.
     * Cada camino se agrega una sola vez (solo cuando el vecino tiene un id mayor).
     */
    public List<double[]> getPathLines() {
        List<double[]> lines = new ArrayList<>();
        for (PathNode node : nodes) {
            for (PathNode neighbor : node.getNeighbors()) {
                if (neighbor.getId() > node.getId()) {
                    lines.add(new double[] {
                        node.getPosition().getX(), node.getPosition().getY(),
                        neighbor.getPosition().getX(), neighbor.getPosition().getY()
                    });
                }
            }
        }
        return lines;
    }

    public List<Zone> getZones() {
        return zones;
    }

    public List<PathNode> getNodes() {
        return nodes;
    }
}
