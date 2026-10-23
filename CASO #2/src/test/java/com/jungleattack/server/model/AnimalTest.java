package com.jungleattack.server.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jungleattack.server.util.Position;

class AnimalTest {

    private PlayerPack playerPack;
    private BotPack botPack;
    private BotPack otherBotPack;

    @BeforeEach
    void setUp() {
        playerPack = new PlayerPack(new Player("Ana", null), Species.JAGUAR);
        botPack = new BotPack(Species.TAPIR, 0);
        otherBotPack = new BotPack(Species.COATI, 1);
    }

    // ----- Energía -----

    @Test
    void damageLowersEnergy() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        bot.receiveDamage(3);
        assertEquals(7, bot.getEnergy());
        assertTrue(bot.isAlive());
    }

    @Test
    void animalDiesAtZeroAndEnergyNeverGoesNegative() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        bot.receiveDamage(10);
        assertEquals(0, bot.getEnergy());
        assertFalse(bot.isAlive());

        bot.receiveDamage(5);
        assertEquals(0, bot.getEnergy());
    }

    @Test
    void healNeverGoesOverMax() {
        PlayerAnimal a = new PlayerAnimal(1, playerPack, new Position(0, 0));
        a.receiveDamage(1);
        a.heal(5, 20);
        assertEquals(20, a.getEnergy());
    }

    // ----- Combate -----

    @Test
    void attackCooldownIsOneSecond() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        PlayerAnimal victim = new PlayerAnimal(2, playerPack, new Position(10, 0));
        List<Animal> enemies = List.of(victim);

        bot.tryAttack(enemies, 0);
        bot.tryAttack(enemies, 500);
        bot.tryAttack(enemies, 999);
        assertEquals(19, victim.getEnergy());

        bot.tryAttack(enemies, 1000);
        assertEquals(18, victim.getEnergy());
    }

    @Test
    void tryAttackReturnsTheAnimalItKilled() {
        PlayerAnimal attacker = new PlayerAnimal(1, playerPack, new Position(0, 0));
        BotAnimal bot = new BotAnimal(2, botPack, new Position(10, 0));
        bot.receiveDamage(8);
        attacker.setOrderedTarget(bot);

        Animal killed = attacker.tryAttack(List.of(bot), 0);
        assertSame(bot, killed);
    }

    @Test
    void botChoosesTheClosestEnemy() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        PlayerAnimal far = new PlayerAnimal(2, playerPack, new Position(30, 0));
        PlayerAnimal near = new PlayerAnimal(3, playerPack, new Position(10, 0));

        bot.tryAttack(List.of(far, near), 0);
        assertEquals(19, near.getEnergy());
        assertEquals(20, far.getEnergy());
    }

    @Test
    void playerAnimalWithoutOrderDoesNotAttack() {
        PlayerAnimal a = new PlayerAnimal(1, playerPack, new Position(0, 0));
        BotAnimal bot = new BotAnimal(2, botPack, new Position(10, 0));
        assertNull(a.tryAttack(List.of(bot), 0));
        assertEquals(10, bot.getEnergy());
    }

    @Test
    void playerAnimalForgetsTargetWhenItLeavesRange() {
        PlayerAnimal a = new PlayerAnimal(1, playerPack, new Position(0, 0));
        BotAnimal bot = new BotAnimal(2, botPack, new Position(10, 0));
        a.setOrderedTarget(bot);

        a.tryAttack(List.of(bot), 0);
        assertEquals(8, bot.getEnergy());

        bot.getPosition().setX(100);
        a.tryAttack(new ArrayList<>(), 1000);
        assertEquals(8, bot.getEnergy());
        assertNull(a.getOrderedTarget());
    }

    @Test
    void botsAreNotEnemiesOfOtherBots() {
        BotAnimal b1 = new BotAnimal(1, botPack, new Position(0, 0));
        BotAnimal b2 = new BotAnimal(2, otherBotPack, new Position(0, 0));
        PlayerAnimal p = new PlayerAnimal(3, playerPack, new Position(0, 0));
        PlayerAnimal sameTeam = new PlayerAnimal(4, playerPack, new Position(0, 0));

        assertFalse(b1.isEnemyOf(b2));
        assertTrue(b1.isEnemyOf(p));
        assertTrue(p.isEnemyOf(b1));
        assertFalse(p.isEnemyOf(sameTeam));
    }

    // ----- Movimiento -----

    @Test
    void moveStepKeepsConstantSpeed() {
        PlayerAnimal a = new PlayerAnimal(1, playerPack, new Position(0, 0));
        a.setRoute(List.of(new Position(1000, 0)));

        a.moveStep(0.5);
        assertEquals(40, a.getPosition().getX(), 0.0001);

        a.moveStep(0.25);
        assertEquals(60, a.getPosition().getX(), 0.0001);
    }

    @Test
    void moveStepTurnsCornersWithoutLosingDistance() {
        PlayerAnimal a = new PlayerAnimal(1, playerPack, new Position(0, 0));
        a.setRoute(List.of(new Position(30, 0), new Position(30, 100)));

        a.moveStep(1);
        assertEquals(30, a.getPosition().getX(), 0.0001);
        assertEquals(50, a.getPosition().getY(), 0.0001);
        assertEquals(1, a.getRoute().size());
    }

    @Test
    void animalStopsAtTheEndOfTheRoute() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        bot.setRoute(List.of(new Position(10, 0)));

        bot.moveStep(10);
        assertEquals(10, bot.getPosition().getX(), 0.0001);
        assertFalse(bot.isMoving());
    }

    @Test
    void botIsSlowerThanPlayerAnimal() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        PlayerAnimal a = new PlayerAnimal(2, playerPack, new Position(0, 0));
        assertTrue(bot.getSpeed() < a.getSpeed());
    }

    @Test
    void playerAnimalKeepsTheOrderWhileWalkingToTheTarget() {
        PlayerAnimal a = new PlayerAnimal(1, playerPack, new Position(0, 0));
        BotAnimal bot = new BotAnimal(2, botPack, new Position(200, 0));
        a.setOrderedTarget(bot);
        a.setRoute(List.of(new Position(180, 0)));

        // Todavía está lejos, pero va caminando: no ataca y no olvida la orden
        assertNull(a.tryAttack(new ArrayList<>(), 0));
        assertSame(bot, a.getOrderedTarget());

        a.moveStep(3);
        a.tryAttack(List.of(bot), 1000);
        assertEquals(8, bot.getEnergy());
    }
}
