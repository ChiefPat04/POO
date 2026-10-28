package com.jungleattack.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.jungleattack.server.map.GameMap;
import com.jungleattack.server.map.PathNode;
import com.jungleattack.server.model.Animal;
import com.jungleattack.server.model.BotPack;
import com.jungleattack.server.model.CoffeeCup;
import com.jungleattack.server.model.Pack;
import com.jungleattack.server.model.Player;
import com.jungleattack.server.model.PlayerAnimal;
import com.jungleattack.server.model.Species;
import com.jungleattack.server.protocol.Message;
import com.jungleattack.server.protocol.MessageType;
import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

class GameSessionTest {

    // "Senders" falsos: en vez de un socket, cada jugador guarda sus mensajes en una lista
    private List<Message> anaMessages;
    private List<Message> betoMessages;
    private Player ana;
    private Player beto;
    private GameSession session;

    @BeforeEach
    void setUp() throws GameException {
        anaMessages = new ArrayList<>();
        betoMessages = new ArrayList<>();
        ana = new Player("Ana", anaMessages::add);
        beto = new Player("Beto", betoMessages::add);
        session = new GameSession("ABCD", ana, new Random(7));
        session.addSecondPlayer(beto);
    }

    // Escoge especies y arranca la partida en t = 0, sin el hilo del tick. 
    private void startGame() throws GameException {
        session.selectSpecies(ana, Species.JAGUAR);
        session.selectSpecies(beto, Species.TAPIR);
        session.startGame(0);
    }

    private Message lastMessage(List<Message> messages) {
        return messages.get(messages.size() - 1);
    }

    private int countZoneUpdates(List<Message> messages) {
        int count = 0;
        for (Message m : messages) {
            if (m.getType() == MessageType.ZONE_UPDATE) {
                count++;
            }
        }
        return count;
    }

    private List<String> eventsOf(Message m) {
        List<String> events = new ArrayList<>();
        for (JsonElement e : m.getArray("events")) {
            events.add(e.getAsString());
        }
        return events;
    }

    private void placeAt(Animal a, Position p) {
        a.getPosition().setX(p.getX());
        a.getPosition().setY(p.getY());
    }

    private List<BotPack> botPacks() {
        List<BotPack> bots = new ArrayList<>();
        for (Pack pack : session.getPacks()) {
            if (pack.isBot()) {
                bots.add((BotPack) pack);
            }
        }
        return bots;
    }

    // Sala de espera

    @Test
    void speciesCannotBeRepeated() throws GameException {
        session.selectSpecies(ana, Species.JAGUAR);
        assertThrows(GameException.class, () -> session.selectSpecies(beto, Species.JAGUAR));
    }

    @Test
    void thirdPlayerCannotJoin() {
        assertThrows(GameException.class, () -> session.addSecondPlayer(new Player("Caro", null)));
    }

    @Test
    void disconnectBeforeSecondPlayerJustEndsTheMatch() {
        List<Message> messages = new ArrayList<>();
        Player alone = new Player("Solo", messages::add);
        GameSession waiting = new GameSession("WXYZ", alone, new Random(1));

        waiting.playerDisconnected(alone);
        assertEquals(MatchState.FINISHED, waiting.getState());
        assertTrue(messages.isEmpty());
    }

    @Test
    void disconnectDuringSelectionGivesTheWinToTheOther() {
        session.playerDisconnected(ana);
        assertEquals(MatchState.FINISHED, session.getState());
        Message over = lastMessage(betoMessages);
        assertEquals(MessageType.GAME_OVER, over.getType());
        assertEquals("Beto", over.getString("winner"));
    }

    // Inicio

    @Test
    void startSendsGameStartedAndFirstZoneUpdate() throws GameException {
        startGame();
        assertEquals(MessageType.GAME_STARTED, anaMessages.get(0).getType());
        assertEquals(MessageType.ZONE_UPDATE, anaMessages.get(1).getType());
        assertEquals(MessageType.GAME_STARTED, betoMessages.get(0).getType());
        assertEquals(MessageType.ZONE_UPDATE, betoMessages.get(1).getType());
    }

    @Test
    void thereAreFiveBotPacksWithTheFreeSpecies() throws GameException {
        startGame();
        assertEquals(7, session.getPacks().size());
        assertEquals(5, botPacks().size());
        for (BotPack bot : botPacks()) {
            assertNotEquals(Species.JAGUAR, bot.getSpecies());
            assertNotEquals(Species.TAPIR, bot.getSpecies());
        }
    }

    @Test
    void theSevenStartingZonesAreDifferent() throws GameException {
        startGame();
        Set<Integer> zones = new HashSet<>();
        zones.add(ana.getViewedZoneId());
        zones.add(beto.getViewedZoneId());
        for (BotPack bot : botPacks()) {
            zones.add(bot.getHomeZoneId());
        }
        assertEquals(7, zones.size());
    }

    // Durante el juego

    @Test
    void coffeeIsTakenOnlyOnce() throws GameException {
        startGame();
        CoffeeCup cup = session.getCups().get(0);
        Animal anaAnimal = ana.getPack().getAliveMembers().get(0);
        placeAt(anaAnimal, cup.getPosition());

        session.tick(50);
        assertTrue(cup.isTaken());
        assertEquals(3, ana.getPack().getDamage());
        assertEquals(1, ana.getCupsTaken());
        assertTrue(eventsOf(lastMessage(anaMessages)).contains(GameConstants.EVENT_MY_CUP));
        assertTrue(eventsOf(lastMessage(betoMessages)).contains(GameConstants.EVENT_RIVAL_CUP));

        // Beto llega al mismo café: ya está tomado, no le da nada
        placeAt(beto.getPack().getAliveMembers().get(0), cup.getPosition());
        session.tick(100);
        assertEquals(3, ana.getPack().getDamage());
        assertEquals(2, beto.getPack().getDamage());
        assertEquals(1, ana.getCupsTaken());
        assertEquals(0, beto.getCupsTaken());
    }

    @Test
    void touchingTheRivalFlagEndsTheGame() throws GameException {
        startGame();
        Animal anaAnimal = ana.getPack().getAliveMembers().get(0);
        placeAt(anaAnimal, beto.getFlag().getPosition());

        session.tick(50);
        assertEquals(MatchState.FINISHED, session.getState());
        assertEquals(MessageType.GAME_OVER, lastMessage(anaMessages).getType());
        assertEquals("Ana", lastMessage(betoMessages).getString("winner"));
    }

    @Test
    void noZoneUpdateIsSentWhenNothingChanges() throws GameException {
        startGame();
        session.tick(50);
        anaMessages.clear();
        betoMessages.clear();

        for (long t = 100; t < 1000; t += 50) {
            session.tick(t);
        }
        assertEquals(0, countZoneUpdates(anaMessages));
        assertEquals(0, countZoneUpdates(betoMessages));

        session.tick(1000);
        assertEquals(1, countZoneUpdates(anaMessages));
        assertEquals(1, countZoneUpdates(betoMessages));
    }

    @Test
    void aFightDoesNotFloodZoneUpdates() throws GameException {
        startGame();
        session.tick(50);
        BotPack bots = botPacks().get(0);
        Animal anaAnimal = ana.getPack().getAliveMembers().get(0);
        anaMessages.clear();

        // Ana queda pegada a un bot durante 5 segundos (100 ticks)
        for (long t = 100; t <= 5050; t += 50) {
            if (anaAnimal.isAlive() && !bots.isEliminated()) {
                Position botPosition = bots.getAliveMembers().get(0).getPosition();
                placeAt(anaAnimal, new Position(botPosition.getX() + 5, botPosition.getY()));
            }
            session.tick(t);
        }
        // Cada ataque es un cambio, así que llegan unos 4 por segundo (el spec dice entre 1 y 5).
        // Antes de la corrección se mandaba uno en cada tick: casi 100 en 5 segundos.
        assertTrue(countZoneUpdates(anaMessages) <= 30, "se mandaron " + countZoneUpdates(anaMessages));
    }

    @Test
    void moveAnimalsWalksOnlyThroughPathNodes() throws GameException {
        startGame();
        Animal anaAnimal = ana.getPack().getAliveMembers().get(0);
        Position click = new Position(anaAnimal.getPosition().getX() + 123, anaAnimal.getPosition().getY() + 77);

        session.moveAnimals(ana, List.of(anaAnimal.getId()), click.getX(), click.getY());

        GameMap map = new GameMap();
        List<Position> route = anaAnimal.getRoute();
        assertFalse(route.isEmpty());
        for (Position p : route) {
            PathNode node = map.findNearestNode(p);
            assertEquals(0, node.getPosition().distanceTo(p), 0.0001);
        }
        Position last = route.get(route.size() - 1);
        assertEquals(0, map.findNearestNode(click).getPosition().distanceTo(last), 0.0001);
    }

    @Test
    void clickBetweenADoorAndTheBorderMakesTheAnimalsCrossToTheNextZone() throws GameException {
        startGame();
        Animal anaAnimal = ana.getPack().getAliveMembers().get(0);
        int startZone = ana.getViewedZoneId();

        // Buscamos una puerta de la zona inicial que tenga su pareja en otra zona
        GameMap map = new GameMap();
        PathNode door = null;
        PathNode otherDoor = null;
        for (PathNode node : map.getZone(startZone).getNodes()) {
            for (PathNode neighbor : node.getNeighbors()) {
                if (neighbor.getZoneId() != startZone) {
                    door = node;
                    otherDoor = neighbor;
                }
            }
        }
        assertNotNull(door);

        // Un clic a un cuarto del camino entre las dos puertas: todavía dentro de la zona inicial
        double clickX = door.getPosition().getX() + (otherDoor.getPosition().getX() - door.getPosition().getX()) / 4;
        double clickY = door.getPosition().getY() + (otherDoor.getPosition().getY() - door.getPosition().getY()) / 4;
        assertEquals(startZone, map.getZoneIdAt(new Position(clickX, clickY)));

        session.moveAnimals(ana, List.of(anaAnimal.getId()), clickX, clickY);

        List<Position> route = anaAnimal.getRoute();
        Position last = route.get(route.size() - 1);
        assertEquals(0, last.distanceTo(otherDoor.getPosition()), 0.0001);
        assertNotEquals(startZone, map.getZoneIdAt(last));
    }

    @Test
    void attackOrderOnAFarEnemyWalksAndKeepsTheOrder() throws GameException {
        startGame();
        PlayerAnimal anaAnimal = (PlayerAnimal) ana.getPack().getAliveMembers().get(0);
        Animal bot = botPacks().get(0).getAliveMembers().get(0);

        session.orderAttack(ana, List.of(anaAnimal.getId()), bot.getId());
        assertSame(bot, anaAnimal.getOrderedTarget());
        assertTrue(anaAnimal.isMoving());
        List<Position> route = anaAnimal.getRoute();
        assertEquals(0, route.get(route.size() - 1).distanceTo(bot.getPosition()), 0.0001);
    }

    @Test
    void attackOrderOnANearEnemyStopsTheAnimal() throws GameException {
        startGame();
        PlayerAnimal anaAnimal = (PlayerAnimal) ana.getPack().getAliveMembers().get(0);
        Animal bot = botPacks().get(0).getAliveMembers().get(0);
        anaAnimal.setRoute(List.of(new Position(0, 0)));
        placeAt(anaAnimal, new Position(bot.getPosition().getX() + 10, bot.getPosition().getY()));

        session.orderAttack(ana, List.of(anaAnimal.getId()), bot.getId());
        assertSame(bot, anaAnimal.getOrderedTarget());
        assertFalse(anaAnimal.isMoving());
    }

    @Test
    void ordersBeforeTheGameStartsAreRejected() {
        assertThrows(GameException.class, () -> session.moveAnimals(ana, List.of(1), 0, 0));
    }
}
