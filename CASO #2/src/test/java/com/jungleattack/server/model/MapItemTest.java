package com.jungleattack.server.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jungleattack.server.util.Position;

/** Pruebas de CoffeeCup, Flag y Player. */
class MapItemTest {

    private final PlayerPack pack = new PlayerPack(new Player("Ana", null), Species.JAGUAR);

    @Test
    void coffeeIsTouchedOnlyWhenCloseEnough() {
        CoffeeCup cup = new CoffeeCup(1, new Position(100, 100));
        PlayerAnimal near = new PlayerAnimal(2, pack, new Position(110, 100));
        PlayerAnimal far = new PlayerAnimal(3, pack, new Position(150, 100));

        assertTrue(cup.isTouchedBy(near));
        assertFalse(cup.isTouchedBy(far));
        assertEquals("coffee.svg", cup.getImageFile());
    }

    @Test
    void deadAnimalDoesNotTouchItems() {
        CoffeeCup cup = new CoffeeCup(1, new Position(100, 100));
        PlayerAnimal a = new PlayerAnimal(2, pack, new Position(100, 100));
        a.receiveDamage(100);
        assertFalse(cup.isTouchedBy(a));
    }

    @Test
    void coffeeCanBeTaken() {
        CoffeeCup cup = new CoffeeCup(1, new Position(0, 0));
        assertFalse(cup.isTaken());
        cup.take();
        assertTrue(cup.isTaken());
    }

    @Test
    void flagImageDependsOnPlayerNumber() {
        Player p1 = new Player("Ana", null);
        Player p2 = new Player("Beto", null);
        Flag red = new Flag(1, new Position(0, 0), p1, 1);
        Flag blue = new Flag(2, new Position(0, 0), p2, 2);

        assertEquals("flag_red.svg", red.getImageFile());
        assertEquals("flag_blue.svg", blue.getImageFile());
        assertEquals(p1, red.getOwner());
    }

    @Test
    void flagHasBiggerRadiusThanCoffee() {
        Flag flag = new Flag(1, new Position(0, 0), null, 1);
        PlayerAnimal a = new PlayerAnimal(2, pack, new Position(25, 0));
        CoffeeCup cup = new CoffeeCup(3, new Position(0, 0));

        assertTrue(flag.isTouchedBy(a));
        assertFalse(cup.isTouchedBy(a));
    }

    @Test
    void botKillBonusEveryTwoBots() {
        Player p = new Player("Ana", null);
        assertFalse(p.addBotKill());
        assertTrue(p.addBotKill());
        assertFalse(p.addBotKill());
        assertTrue(p.addBotKill());
        assertEquals(4, p.getBotsKilled());
    }
}
