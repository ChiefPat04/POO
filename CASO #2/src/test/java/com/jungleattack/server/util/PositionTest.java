package com.jungleattack.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void distanceUsesPythagoras() {
        Position a = new Position(0, 0);
        Position b = new Position(3, 4);
        assertEquals(5, a.distanceTo(b), 0.0001);
    }

    @Test
    void moveTowardsAdvancesTheGivenDistance() {
        Position p = new Position(0, 0);
        p.moveTowards(new Position(10, 0), 4);
        assertEquals(4, p.getX(), 0.0001);
        assertEquals(0, p.getY(), 0.0001);
    }

    @Test
    void moveTowardsDoesNotGoPastTheTarget() {
        Position p = new Position(0, 0);
        Position target = new Position(3, 4);
        p.moveTowards(target, 100);
        assertEquals(3, p.getX(), 0.0001);
        assertEquals(4, p.getY(), 0.0001);
    }

    @Test
    void copyIsIndependentFromTheOriginal() {
        Position original = new Position(1, 2);
        Position copy = new Position(original);
        copy.setX(50);
        assertEquals(1, original.getX(), 0.0001);
    }
}
