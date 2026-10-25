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

    // La zona 0 tiene su puerta de la derecha en (760, 300) y la zona 1 la suya en (840, 300)

    @Test
    void clickBetweenTheDoorAndTheBorderGoesToTheNextZone() {
        PathNode node = map.findClickNode(new Position(790, 300));
        assertEquals(840, node.getPosition().getX(), 0.0001);
        assertEquals(300, node.getPosition().getY(), 0.0001);
        assertEquals(1, node.getZoneId());
    }

    @Test
    void clickOnTheInsideSideOfTheDoorStaysInTheZone() {
        PathNode node = map.findClickNode(new Position(700, 300));
        assertEquals(760, node.getPosition().getX(), 0.0001);
        assertEquals(0, node.getZoneId());
    }

    @Test
    void clickFarFromTheDoorStaysInTheZone() {
        // Está en el borde derecho pero lejos de la puerta (a 200 píxeles hacia arriba)
        PathNode node = map.findClickNode(new Position(790, 100));
        assertEquals(0, node.getZoneId());
    }

    @Test
    void clickNearTheBottomDoorGoesDown() {
        // La puerta de abajo de la zona 0 está en (400, 560) y la de la zona 4 en (400, 640)
        PathNode node = map.findClickNode(new Position(400, 590));
        assertEquals(400, node.getPosition().getX(), 0.0001);
        assertEquals(640, node.getPosition().getY(), 0.0001);
        assertEquals(4, node.getZoneId());
    }

    @Test
    void clickNearTheLeftDoorOfTheNextZoneGoesBack() {
        // Viendo la zona 1, un clic entre el borde izquierdo y su puerta (840, 300) vuelve a la zona 0
        PathNode node = map.findClickNode(new Position(810, 300));
        assertEquals(760, node.getPosition().getX(), 0.0001);
        assertEquals(0, node.getZoneId());
    }

    @Test
    void clickAtTheMapBorderWithoutNeighborStaysInTheZone() {
        // La zona 0 no tiene vecino a la izquierda, ni puerta ahí
        assertSame(map.getZone(0).getCenter(), map.findClickNode(new Position(10, 300)));
    }
}
