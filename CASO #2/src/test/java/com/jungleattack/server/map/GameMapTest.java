package com.jungleattack.server.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jungleattack.server.util.Position;

class GameMapTest {

    private final GameMap map = new GameMap();

    @Test
    void mapHasTwelveZones() {
        assertEquals(12, map.getZones().size());
    }

    @Test
    void cornerEdgeAndMiddleZonesHaveTheRightNumberOfDoors() {
        // nodos = 1 centro + puertas
        assertEquals(1 + 2, map.getZone(0).getNodes().size());
        assertEquals(1 + 3, map.getZone(1).getNodes().size());
        assertEquals(1 + 4, map.getZone(5).getNodes().size());
        assertEquals(1 + 2, map.getZone(11).getNodes().size());
    }

    @Test
    void doorAndLineCounts() {
        assertEquals(34, map.getDoorNodes().size());
        assertEquals(12 + 34, map.getNodes().size());
        assertEquals(51, map.getPathLines().size());
    }

    @Test
    void everyNodeIsInsideItsZone() {
        for (PathNode node : map.getNodes()) {
            Zone zone = map.getZone(node.getZoneId());
            assertTrue(zone.contains(node.getPosition()));
        }
    }

    @Test
    void zoneIdAtPosition() {
        assertEquals(0, map.getZoneIdAt(new Position(10, 10)));
        assertEquals(1, map.getZoneIdAt(new Position(800, 0)));
        assertEquals(5, map.getZoneIdAt(new Position(900, 700)));
        assertEquals(11, map.getZoneIdAt(new Position(3199, 1799)));
        assertEquals(11, map.getZoneIdAt(new Position(5000, 5000)));
    }

    @Test
    void zoneCenterIsInTheMiddle() {
        Position c = map.getZone(5).getCenter().getPosition();
        assertEquals(1200, c.getX(), 0.0001);
        assertEquals(900, c.getY(), 0.0001);
    }

    @Test
    void findNearestNode() {
        PathNode center0 = map.getZone(0).getCenter();
        assertSame(center0, map.findNearestNode(new Position(410, 290)));
    }
}
