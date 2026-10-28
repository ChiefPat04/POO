package com.jungleattack.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jungleattack.server.model.Animal;
import com.jungleattack.server.model.BotAnimal;
import com.jungleattack.server.model.BotPack;
import com.jungleattack.server.model.Player;
import com.jungleattack.server.model.PlayerAnimal;
import com.jungleattack.server.model.PlayerPack;
import com.jungleattack.server.model.Species;
import com.jungleattack.server.util.Position;

class CombatManagerTest {

    private CombatManager combat;
    private Player ana;
    private PlayerPack playerPack;
    private BotPack botPack;

    @BeforeEach
    void setUp() {
        combat = new CombatManager();
        ana = new Player("Ana", null);
        playerPack = new PlayerPack(ana, Species.JAGUAR);
        botPack = new BotPack(Species.TAPIR, 0);
    }

    // Corre processAttacks cada 50 ms, como lo hace el tick, desde "from" hasta "to". 
    private void runTicks(List<Animal> all, long from, long to) {
        for (long t = from; t <= to; t += 50) {
            combat.processAttacks(all, t);
        }
    }

    @Test
    void exampleFromTheAssignment() {
        // 1 animal del jugador (20 de energía) rodeado por 4 bots (10 de energía cada uno)
        PlayerAnimal hero = new PlayerAnimal(1, playerPack, new Position(0, 0));
        List<BotAnimal> bots = new ArrayList<>();
        bots.add(new BotAnimal(2, botPack, new Position(10, 0)));
        bots.add(new BotAnimal(3, botPack, new Position(-10, 0)));
        bots.add(new BotAnimal(4, botPack, new Position(0, 10)));
        bots.add(new BotAnimal(5, botPack, new Position(0, -10)));
        hero.setOrderedTarget(bots.get(0));

        // Como en GameSession, los animales de los jugadores van primero en la lista
        List<Animal> all = new ArrayList<>();
        all.add(hero);
        all.addAll(bots);

        // Ataques en t = 0, 1, 2, 3 y 4 segundos: el héroe hace 5 x 2 = 10 de daño
        runTicks(all, 0, 4000);
        assertFalse(bots.get(0).isAlive());
        assertEquals(1, ana.getBotsKilled());
        // Recibió 4 x 4 = 16 en los primeros 4 segundos, y 3 más en t = 4 (ya solo quedan 3 bots)
        assertEquals(1, hero.getEnergy());

        // En t = 5 los 3 bots que quedan lo terminan
        runTicks(all, 4050, 5000);
        assertFalse(hero.isAlive());
    }

    @Test
    void botsDoNotAttackEachOther() {
        BotPack otherBots = new BotPack(Species.COATI, 1);
        BotAnimal b1 = new BotAnimal(1, botPack, new Position(0, 0));
        BotAnimal b2 = new BotAnimal(2, otherBots, new Position(10, 0));
        List<Animal> all = List.of(b1, b2);

        assertFalse(combat.processAttacks(all, 0));
        assertEquals(10, b1.getEnergy());
        assertEquals(10, b2.getEnergy());
    }

    @Test
    void returnsTrueOnlyWhenSomeoneAttacked() {
        BotAnimal bot = new BotAnimal(1, botPack, new Position(0, 0));
        PlayerAnimal far = new PlayerAnimal(2, playerPack, new Position(100, 0));
        List<Animal> all = List.of(bot, far);
        assertFalse(combat.processAttacks(all, 0));

        far.getPosition().setX(20);
        assertTrue(combat.processAttacks(all, 1000));
    }

    @Test
    void bonusEveryTwoBotsHealsThePack() {
        // El héroe tiene que ser miembro del pack para que healAll lo cure
        playerPack.createMembers(new Position(1000, 1000), 1, new Random(1));
        Animal hero = playerPack.getMembers().get(0);
        hero.getPosition().setX(0);
        hero.getPosition().setY(0);
        hero.receiveDamage(5);

        BotAnimal bot1 = new BotAnimal(20, botPack, new Position(10, 0));
        BotAnimal bot2 = new BotAnimal(21, botPack, new Position(-10, 0));
        bot1.receiveDamage(9);
        bot2.receiveDamage(9);
        List<Animal> all = List.of(hero, bot1, bot2);

        // t = 0: mata al primer bot (todavía no hay bono) y el segundo le quita 1
        ((PlayerAnimal) hero).setOrderedTarget(bot1);
        combat.processAttacks(all, 0);
        assertEquals(1, ana.getBotsKilled());
        assertEquals(14, hero.getEnergy());

        // t = 1: mata al segundo bot -> 2 bots -> bono de +1 a todo el pack
        ((PlayerAnimal) hero).setOrderedTarget(bot2);
        combat.processAttacks(all, 1000);
        assertEquals(2, ana.getBotsKilled());
        assertEquals(15, hero.getEnergy());
    }

    @Test
    void findEnemiesInRangeSkipsFriendsDeadAndFarAnimals() {
        PlayerAnimal me = new PlayerAnimal(1, playerPack, new Position(0, 0));
        PlayerAnimal friend = new PlayerAnimal(2, playerPack, new Position(5, 0));
        BotAnimal near = new BotAnimal(3, botPack, new Position(10, 0));
        BotAnimal dead = new BotAnimal(4, botPack, new Position(10, 0));
        BotAnimal far = new BotAnimal(5, botPack, new Position(100, 0));
        dead.receiveDamage(100);

        List<Animal> enemies = combat.findEnemiesInRange(me, List.of(me, friend, near, dead, far));
        assertEquals(1, enemies.size());
        assertEquals(near, enemies.get(0));
    }
}
