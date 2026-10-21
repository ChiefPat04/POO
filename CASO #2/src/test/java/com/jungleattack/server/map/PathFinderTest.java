package com.jungleattack.server.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.jungleattack.server.util.Position;

class PathFinderTest {

    private final GameMap map = new GameMap();

    private void assertSamePoint(Position expected, Position actual) {
        assertEquals(expected.getX(), actual.getX(), 0.0001);
        assertEquals(expected.getY(), actual.getY(), 0.0001);
    }

    @Test
    void routeBetweenNeighborZones() {
        PathNode start = map.getZone(0).getCenter();
        PathNode end = map.getZone(1).getCenter();
        List<Position> route = PathFinder.findRoute(start, end);

        assertEquals(4, route.size());
        assertSamePoint(start.getPosition(), route.get(0));
        assertSamePoint(new Position(760, 300), route.get(1));
        assertSamePoint(new Position(840, 300), route.get(2));
        assertSamePoint(end.getPosition(), route.get(3));
    }

    @Test
    void routeBetweenOppositeCorners() {
        PathNode start = map.getZone(0).getCenter();
        PathNode end = map.getZone(11).getCenter();
        List<Position> route = PathFinder.findRoute(start, end);

        assertEquals(1 + 5 * 3, route.size());
        assertSamePoint(start.getPosition(), route.get(0));
        assertSamePoint(end.getPosition(), route.get(route.size() - 1));
    }

    @Test
    void routeToTheSameNodeHasOnePoint() {
        PathNode node = map.getZone(3).getCenter();
        assertEquals(1, PathFinder.findRoute(node, node).size());
    }

    @Test
    void allTwelveZonesAreConnected() {
        PathNode start = map.getZone(0).getCenter();
        for (Zone zone : map.getZones()) {
            List<Position> route = PathFinder.findRoute(start, zone.getCenter());
            assertFalse(route.isEmpty(), "No hay ruta a la zona " + zone.getId());
        }
    }

    @Test
    void routeInsideAZoneDoesNotLeaveIt() {
        Zone zone = map.getZone(5);
        for (PathNode a : zone.getNodes()) {
            for (PathNode b : zone.getNodes()) {
                for (Position p : PathFinder.findRoute(a, b)) {
                    assertEquals(5, map.getZoneIdAt(p));
                }
            }
        }
    }

    @Test
    void routeReturnsCopiesOfThePositions() {
        PathNode node = map.getZone(0).getCenter();
        List<Position> route = PathFinder.findRoute(node, node);
        route.get(0).setX(-1);
        assertEquals(400, node.getPosition().getX(), 0.0001);
    }
}
