package com.jungleattack.server.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.jungleattack.server.map.GameMap;
import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

class PackTest {

    @Test
    void playerPackHasTenAnimalsWithEnergy20AndDamage2() {
        PlayerPack pack = new PlayerPack(new Player("Ana", null), Species.JAGUAR);
        pack.createMembers(new Position(400, 300), 1, new Random(1));

        assertEquals(10, pack.getMembers().size());
        assertEquals(2, pack.getDamage());
        for (Animal a : pack.getMembers()) {
            assertEquals(20, a.getEnergy());
            assertTrue(a instanceof PlayerAnimal);
        }
    }

    @Test
    void botPackHasFourAnimalsWithEnergy10AndDamage1() {
        BotPack pack = new BotPack(Species.TAPIR, 3);
        pack.createMembers(new Position(400, 300), 1, new Random(1));

        assertEquals(4, pack.getMembers().size());
        assertEquals(1, pack.getDamage());
        for (Animal a : pack.getMembers()) {
            assertEquals(10, a.getEnergy());
            assertTrue(a instanceof BotAnimal);
        }
    }

    @Test
    void membersAreCreatedAroundTheCenterWithConsecutiveIds() {
        BotPack pack = new BotPack(Species.TAPIR, 0);
        Position center = new Position(400, 300);
        pack.createMembers(center, 50, new Random(7));

        List<Animal> members = pack.getMembers();
        for (int i = 0; i < members.size(); i++) {
            assertEquals(50 + i, members.get(i).getId());
            Position p = members.get(i).getPosition();
            assertTrue(Math.abs(p.getX() - 400) <= GameConstants.SPAWN_SPREAD);
            assertTrue(Math.abs(p.getY() - 300) <= GameConstants.SPAWN_SPREAD);
        }
    }

    @Test
    void coffeeGivesPlusOneDamageToTheWholePack() {
        PlayerPack pack = new PlayerPack(new Player("Ana", null), Species.JAGUAR);
        pack.createMembers(new Position(0, 0), 1, new Random(1));
        pack.increaseDamage(GameConstants.COFFEE_DAMAGE_BONUS);
        assertEquals(3, pack.getDamage());

        BotPack bots = new BotPack(Species.TAPIR, 0);
        for (Animal a : pack.getMembers()) {
            BotAnimal victim = new BotAnimal(999, bots, a.getPosition());
            ((PlayerAnimal) a).setOrderedTarget(victim);
            a.tryAttack(List.of(victim), 0);
            assertEquals(7, victim.getEnergy());
        }
    }

    @Test
    void healAllNeverGoesOver20AndSkipsDeadAnimals() {
        PlayerPack pack = new PlayerPack(new Player("Ana", null), Species.JAGUAR);
        pack.createMembers(new Position(0, 0), 1, new Random(1));
        List<Animal> members = pack.getMembers();
        members.get(0).receiveDamage(5);
        members.get(1).receiveDamage(20);

        pack.healAll(1);

        assertEquals(16, members.get(0).getEnergy());
        assertEquals(0, members.get(1).getEnergy());
        assertEquals(20, members.get(2).getEnergy());
    }

    @Test
    void countDeadAndEliminated() {
        BotPack pack = new BotPack(Species.TAPIR, 0);
        pack.createMembers(new Position(0, 0), 1, new Random(1));
        assertEquals(0, pack.countDead());
        assertFalse(pack.isEliminated());

        for (Animal a : pack.getMembers()) {
            a.receiveDamage(100);
        }
        assertEquals(4, pack.countDead());
        assertTrue(pack.isEliminated());
    }

    @Test
    void findAnimalById() {
        BotPack pack = new BotPack(Species.TAPIR, 0);
        pack.createMembers(new Position(0, 0), 10, new Random(1));
        assertSame(pack.getMembers().get(2), pack.findAnimal(12));
        assertNull(pack.findAnimal(99));
    }

    @Test
    void patrolNeverLeavesItsZone() {
        GameMap map = new GameMap();
        Random random = new Random(42);

        int[] zonesToTest = {0, 2, 5};
        for (int zoneId : zonesToTest) {
            BotPack pack = new BotPack(Species.TAPIR, zoneId);
            pack.createMembers(map.getZone(zoneId).getCenter().getPosition(), 1, random);

            int timesStarted = 0;
            for (long now = 0; now < 300_000; now += GameConstants.TICK_MS) {
                if (pack.updatePatrol(map, random, now)) {
                    timesStarted++;
                }
                for (Animal bot : pack.getAliveMembers()) {
                    bot.moveStep(GameConstants.TICK_MS / 1000.0);
                    assertEquals(zoneId, map.getZoneIdAt(bot.getPosition()));
                }
            }
            assertTrue(timesStarted > 10, "el pack debería caminar varias veces");
        }
    }

    @Test
    void patrolWaitsBeforeWalkingAgain() {
        GameMap map = new GameMap();
        Random random = new Random(1);
        BotPack pack = new BotPack(Species.TAPIR, 5);
        pack.createMembers(map.getZone(5).getCenter().getPosition(), 1, random);

        assertTrue(pack.updatePatrol(map, random, 0));

        assertFalse(pack.updatePatrol(map, random, 50));

        for (Animal bot : pack.getMembers()) {
            bot.moveStep(100);
            assertFalse(bot.isMoving());
        }

        assertFalse(pack.updatePatrol(map, random, 1000));
        assertFalse(pack.updatePatrol(map, random, 50 + GameConstants.BOT_WAIT_MS - 1));
        assertTrue(pack.updatePatrol(map, random, 50 + GameConstants.BOT_WAIT_MS));
    }

    @Test
    void pauseForDelaysThePatrol() {
        GameMap map = new GameMap();
        Random random = new Random(1);
        BotPack pack = new BotPack(Species.TAPIR, 5);
        pack.createMembers(map.getZone(5).getCenter().getPosition(), 1, random);

        pack.pauseFor(5000);
        assertFalse(pack.updatePatrol(map, random, 5000 + GameConstants.BOT_WAIT_MS - 1));
        assertTrue(pack.updatePatrol(map, random, 5000 + GameConstants.BOT_WAIT_MS));
    }
}
