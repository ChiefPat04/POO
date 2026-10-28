package com.jungleattack.server.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.jungleattack.server.map.GameMap;
import com.jungleattack.server.map.PathFinder;
import com.jungleattack.server.map.PathNode;
import com.jungleattack.server.model.Animal;
import com.jungleattack.server.model.BotPack;
import com.jungleattack.server.model.CoffeeCup;
import com.jungleattack.server.model.Flag;
import com.jungleattack.server.model.MapItem;
import com.jungleattack.server.model.Pack;
import com.jungleattack.server.model.Player;
import com.jungleattack.server.model.PlayerAnimal;
import com.jungleattack.server.model.PlayerPack;
import com.jungleattack.server.model.Species;
import com.jungleattack.server.protocol.Message;
import com.jungleattack.server.protocol.MessageType;
import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Una partida entre dos jugadores.
 * Los métodos públicos son synchronized: el hilo del tick y los hilos de los jugadores no se pisan.
 */
public class GameSession {

    private final String code;
    private Player player1;
    private Player player2;
    private MatchState state;
    private GameMap map;
    private final List<Pack> packs;
    private final List<CoffeeCup> cups;
    private final Random random;
    private final CombatManager combat;
    private ScheduledExecutorService loop;
    private int nextId;
    private long startTime;
    private long lastTickTime;
    private long lastSyncTime;
    private boolean changed;

    public GameSession(String code, Player creator, Random random) {
        this.code = code;
        this.player1 = creator;
        this.random = random;
        this.state = MatchState.WAITING_FOR_PLAYER;
        this.packs = new ArrayList<>();
        this.cups = new ArrayList<>();
        this.combat = new CombatManager();
        this.nextId = 1;
    }

    // Sala de espera 

    public synchronized void addSecondPlayer(Player p) throws GameException {
        if (player2 != null || state != MatchState.WAITING_FOR_PLAYER) {
            throw new GameException(GameConstants.ERROR_MATCH_FULL);
        }
        player2 = p;
        state = MatchState.SELECTING_ANIMALS;
    }

    public synchronized boolean isFull() {
        return player2 != null;
    }

    // true cuando los dos ya escogieron especie. 
    public synchronized boolean bothSelected() {
        return player2 != null && player1.getSpecies() != null && player2.getSpecies() != null;
    }

    public synchronized void selectSpecies(Player p, Species species) throws GameException {
        if (state != MatchState.SELECTING_ANIMALS) {
            throw new GameException(GameConstants.ERROR_NOT_PLAYING);
        }
        if (otherPlayer(p).getSpecies() == species) {
            throw new GameException(GameConstants.ERROR_SPECIES_TAKEN);
        }
        p.setSpecies(species);
    }

    // Manda MATCH_JOINED a los dos: cada uno ve el nombre del otro. 
    public synchronized void announceJoined() {
        player1.send(new Message(MessageType.MATCH_JOINED)
                .put("code", code).put("opponent", player2.getNickname()));
        player2.send(new Message(MessageType.MATCH_JOINED)
                .put("code", code).put("opponent", player1.getNickname()));
    }

    /**
     * Manda ANIMAL_SELECTION a los dos: las especies que puede escoger cada uno
     * (todas menos la del rival) y las especies que ya escogieron.
     */
    public synchronized void announceSelection() {
        for (Player p : players()) {
            Player opponent = otherPlayer(p);
            List<Species> available = new ArrayList<>();
            for (Species s : Species.values()) {
                if (s != opponent.getSpecies()) {
                    available.add(s);
                }
            }
            p.send(new Message(MessageType.ANIMAL_SELECTION)
                    .put("available", available)
                    .put("mySpecies", p.getSpecies())
                    .put("opponentSpecies", opponent.getSpecies()));
        }
    }

    // Inicio 

    /**
     * Crea el mapa, los packs, las banderas y los cafés.
     * Manda GAME_STARTED y el primer ZONE_UPDATE a los dos.
     * No arranca el tick: para eso está startLoop(). Así los tests pueden llamar tick() a mano.
     */
    public synchronized void startGame(long now) throws GameException {
        if (state != MatchState.SELECTING_ANIMALS || !bothSelected()) {
            throw new GameException(GameConstants.ERROR_NOT_PLAYING);
        }
        map = new GameMap();

        // Revolver las 12 zonas: 2 para los jugadores, 5 para los bots
        List<Integer> zoneIds = new ArrayList<>();
        for (int i = 0; i < GameConstants.ZONE_COUNT; i++) {
            zoneIds.add(i);
        }
        Collections.shuffle(zoneIds, random);

        addPlayerPack(player1, zoneIds.get(0), 1);
        addPlayerPack(player2, zoneIds.get(1), 2);
        addBotPacks(zoneIds);
        addCups();

        state = MatchState.PLAYING;
        startTime = now;
        lastTickTime = now;
        lastSyncTime = now;
        changed = true;

        for (Player p : players()) {
            sendGameStarted(p, otherPlayer(p));
        }
        sendUpdates(now);
    }

    // Arranca el hilo que llama tick() cada TICK_MS. Lo llama el servidor después de startGame(). 
    public synchronized void startLoop() {
        if (loop != null || state != MatchState.PLAYING) {
            return;
        }
        loop = Executors.newSingleThreadScheduledExecutor();
        loop.scheduleAtFixedRate(this::safeTick,
                GameConstants.TICK_MS, GameConstants.TICK_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Llama a tick() atrapando cualquier error.
     * Si un error se escapara, Java cancelaría el tick para siempre sin avisar y el juego se congelaría.
     */
    private void safeTick() {
        try {
            tick(System.currentTimeMillis());
        } catch (RuntimeException e) {
            System.err.println("Error en el tick de la partida " + code);
            e.printStackTrace();
        }
    }

    private void addPlayerPack(Player p, int zoneId, int playerNumber) {
        PlayerPack pack = new PlayerPack(p, p.getSpecies());
        p.setPack(pack);
        pack.createMembers(zoneCenter(zoneId), nextId, random);
        nextId += pack.getSize();
        packs.add(pack);

        p.setFlag(new Flag(nextId, zoneCenter(zoneId), p, playerNumber));
        nextId++;
        p.setViewedZoneId(zoneId);
    }

    private void addBotPacks(List<Integer> zoneIds) {
        // Los bots usan las especies que los jugadores no escogieron
        List<Species> free = new ArrayList<>();
        for (Species s : Species.values()) {
            if (s != player1.getSpecies() && s != player2.getSpecies()) {
                free.add(s);
            }
        }
        for (int i = 0; i < GameConstants.BOT_PACK_COUNT; i++) {
            BotPack pack = new BotPack(free.get(i), zoneIds.get(2 + i));
            pack.createMembers(zoneCenter(pack.getHomeZoneId()), nextId, random);
            nextId += pack.getSize();
            packs.add(pack);
        }
    }

    // Pone los cafés en puertas al azar. 
    private void addCups() {
        List<PathNode> doors = new ArrayList<>(map.getDoorNodes());
        Collections.shuffle(doors, random);
        for (int i = 0; i < GameConstants.COFFEE_CUP_COUNT; i++) {
            cups.add(new CoffeeCup(nextId, doors.get(i).getPosition()));
            nextId++;
        }
    }

    private void sendGameStarted(Player p, Player opponent) {
        Message msg = new Message(MessageType.GAME_STARTED)
                .put("columns", GameConstants.ZONE_COLUMNS)
                .put("rows", GameConstants.ZONE_ROWS)
                .put("zoneWidth", GameConstants.ZONE_WIDTH)
                .put("zoneHeight", GameConstants.ZONE_HEIGHT)
                .put("paths", map.getPathLines())
                .put("myZoneId", p.getViewedZoneId())
                .put("mySpecies", p.getSpecies().name())
                .put("opponent", opponent.getNickname());
        p.send(msg);
    }

    // Órdenes de los jugadores 

    // Manda a los animales al nodo más cercano al clic, por los caminos.
    // Si el clic cae entre una puerta y el borde, van a la puerta de la zona de al lado (ver GameMap.findClickNode).
    public synchronized void moveAnimals(Player p, List<Integer> ids, double x, double y) throws GameException {
        checkPlaying();
        Position target = new Position(x, y);
        for (int id : ids) {
            PlayerAnimal a = findOwnAnimal(p, id);
            if (a != null) {
                a.setOrderedTarget(null);
                a.setRoute(routeToNode(a, map.findClickNode(target)));
            }
        }
        changed = true;
    }

    /**
     * Ordena atacar a un enemigo.
     * Los animales que ya están en rango se detienen y atacan.
     * Los que están lejos caminan hacia el enemigo y atacan cuando llegan.
     */
    public synchronized void orderAttack(Player p, List<Integer> ids, int targetId) throws GameException {
        checkPlaying();
        Animal target = findEnemyAnimal(p, targetId);
        for (int id : ids) {
            PlayerAnimal a = findOwnAnimal(p, id);
            if (a == null) {
                continue;
            }
            a.setOrderedTarget(target);
            if (a.isInRange(target)) {
                a.stopMoving();
            } else {
                // Por los caminos hasta el nodo más cercano al enemigo, y de ahí hasta el enemigo
                List<Position> route = routeTo(a, target.getPosition());
                route.add(new Position(target.getPosition()));
                a.setRoute(route);
            }
        }
        changed = true;
    }

    public synchronized void changeViewedZone(Player p, int zoneId) throws GameException {
        checkPlaying();
        if (zoneId < 0 || zoneId >= GameConstants.ZONE_COUNT) {
            throw new GameException(GameConstants.ERROR_BAD_MESSAGE);
        }
        p.setViewedZoneId(zoneId);
        changed = true;
    }

    /**
     * Un jugador se desconectó.
     * Si el otro todavía no había entrado, la partida solo termina (MatchManager la borra).
     * Si el otro ya había entrado, gana el otro.
     */
    public synchronized void playerDisconnected(Player p) {
        if (state == MatchState.WAITING_FOR_PLAYER) {
            state = MatchState.FINISHED;
        } else if (state == MatchState.SELECTING_ANIMALS || state == MatchState.PLAYING) {
            endGame(otherPlayer(p), GameConstants.REASON_DISCONNECTED);
        }
    }

    // El tick: se ejecuta cada TICK_MS 

    public synchronized void tick(long now) {
        if (state != MatchState.PLAYING) {
            return;
        }
        double seconds = (now - lastTickTime) / 1000.0;
        lastTickTime = now;
        List<Animal> all = allAnimals();

        updateBots(now);
        stopAnimalsInFight(all, now);
        moveAll(all, seconds);
        checkCoffeeCups();
        fight(all, now);
        checkFlags();
        if (state != MatchState.PLAYING) {
            return;
        }
        checkEliminatedPlayers();
        if (state != MatchState.PLAYING) {
            return;
        }
        keepViewedZonesWithAnimals();
        sendUpdates(now);
    }

    private void updateBots(long now) {
        for (Pack pack : packs) {
            if (pack.isBot() && ((BotPack) pack).updatePatrol(map, random, now)) {
                changed = true;
            }
        }
    }

    /**
     * Un bot con un enemigo en rango se detiene, y su pack deja de patrullar por un rato.
     * Un animal del jugador que ya llegó a su objetivo también se detiene.
     */
    private void stopAnimalsInFight(List<Animal> all, long now) {
        for (Animal a : all) {
            if (!a.isAlive()) {
                continue;
            }
            if (a.isBot()) {
                if (!combat.findEnemiesInRange(a, all).isEmpty()) {
                    a.stopMoving();
                    ((BotPack) a.getPack()).pauseFor(now);
                }
            } else {
                Animal target = ((PlayerAnimal) a).getOrderedTarget();
                if (target != null && target.isAlive() && a.isInRange(target)) {
                    a.stopMoving();
                }
            }
        }
    }

    private void moveAll(List<Animal> all, double seconds) {
        for (Animal a : all) {
            if (a.isAlive()) {
                a.moveStep(seconds);
            }
        }
    }

    // El primer pack que toca un café que nadie ha tomado, se lo queda.
    private void checkCoffeeCups() {
        for (CoffeeCup cup : cups) {
            if (cup.isTaken()) {
                continue;
            }
            for (Player p : players()) {
                if (touches(cup, p.getPack())) {
                    cup.take();
                    p.getPack().increaseDamage(GameConstants.COFFEE_DAMAGE_BONUS);
                    p.addCup();
                    p.addEvent(GameConstants.EVENT_MY_CUP);
                    otherPlayer(p).addEvent(GameConstants.EVENT_RIVAL_CUP);
                    changed = true;
                    break;
                }
            }
        }
    }

    // Los ataques, y los eventos de bots eliminados, bonos y animales perdidos. 
    private void fight(List<Animal> all, long now) {
        int kills1 = player1.getBotsKilled();
        int kills2 = player2.getBotsKilled();
        int dead1 = player1.getPack().countDead();
        int dead2 = player2.getPack().countDead();

        if (combat.processAttacks(all, now)) {
            changed = true;
        }

        addCombatEvents(player1, kills1, dead1);
        addCombatEvents(player2, kills2, dead2);
    }

    // Compara cuántos bots mató y cuántos animales perdió el jugador antes y después del combate. 
    private void addCombatEvents(Player p, int killsBefore, int deadBefore) {
        for (int k = killsBefore + 1; k <= p.getBotsKilled(); k++) {
            p.addEvent(GameConstants.EVENT_BOT_KILLED);
            if (k % GameConstants.BOTS_KILLED_FOR_BONUS == 0) {
                p.addEvent(GameConstants.EVENT_BONUS);
            }
        }
        for (int d = deadBefore; d < p.getPack().countDead(); d++) {
            p.addEvent(GameConstants.EVENT_ANIMAL_LOST);
        }
    }

    // Si un animal del rival toca tu bandera, el rival gana. 
    private void checkFlags() {
        checkFlagTouched(player1, player2);
        checkFlagTouched(player2, player1);
    }

    private void checkFlagTouched(Player owner, Player rival) {
        if (touches(owner.getFlag(), rival.getPack())) {
            endGame(rival, GameConstants.REASON_FLAG);
        }
    }

    private void checkEliminatedPlayers() {
        if (player1.getPack().isEliminated()) {
            endGame(player2, GameConstants.REASON_ELIMINATED);
        } else if (player2.getPack().isEliminated()) {
            endGame(player1, GameConstants.REASON_ELIMINATED);
        }
    }

    // Si el jugador ya no tiene animales en la zona que ve, se le cambia a una donde sí tenga. 
    private void keepViewedZonesWithAnimals() {
        for (Player p : players()) {
            List<Animal> alive = p.getPack().getAliveMembers();
            if (alive.isEmpty() || hasOwnAnimalIn(p, p.getViewedZoneId())) {
                continue;
            }
            p.setViewedZoneId(map.getZoneIdAt(alive.get(0).getPosition()));
            p.addEvent(GameConstants.EVENT_ZONE_CHANGED);
            changed = true;
        }
    }

    // Avisar a los jugadores 

    // Manda ZONE_UPDATE solo si algo cambió, si cambió lo que puede ver, o si toca el chequeo de cada segundo. 
    private void sendUpdates(long now) {
        boolean syncDue = now - lastSyncTime >= GameConstants.SYNC_MS;
        if (syncDue) {
            lastSyncTime = now;
        }
        for (Player p : players()) {
            List<Integer> visible = idsOf(visibleAnimals(p));
            if (changed || syncDue || !visible.equals(p.getLastVisibleIds())) {
                sendZoneUpdate(p);
                p.setLastVisibleIds(visible);
            }
        }
        changed = false;
    }

    private void sendZoneUpdate(Player p) {
        List<Map<String, Object>> animals = new ArrayList<>();
        for (Animal a : visibleAnimals(p)) {
            animals.add(animalData(a, p));
        }
        Message msg = new Message(MessageType.ZONE_UPDATE)
                .put("viewedZoneId", p.getViewedZoneId())
                .put("myZones", myZones(p))
                .put("animals", animals)
                .put("items", itemsIn(p.getViewedZoneId()))
                .put("myDamage", p.getPack().getDamage())
                .put("myCups", p.getCupsTaken())
                .put("myBotsKilled", p.getBotsKilled())
                .put("seconds", elapsedSeconds())
                .put("events", p.takeEvents());
        p.send(msg);
    }

    // Los animales que el jugador puede ver: los suyos y los de la zona que mira. 
    private List<Animal> visibleAnimals(Player p) {
        List<Animal> visible = new ArrayList<>();
        for (Animal a : allAnimals()) {
            if (!a.isAlive()) {
                continue;
            }
            boolean mine = a.getPack() == p.getPack();
            boolean inViewedZone = map.getZoneIdAt(a.getPosition()) == p.getViewedZoneId();
            if (mine || inViewedZone) {
                visible.add(a);
            }
        }
        return visible;
    }

    private Map<String, Object> animalData(Animal a, Player viewer) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", a.getId());
        data.put("species", a.getSpecies().name());
        data.put("x", a.getPosition().getX());
        data.put("y", a.getPosition().getY());
        data.put("energy", a.getEnergy());
        data.put("mine", a.getPack() == viewer.getPack());
        data.put("bot", a.isBot());
        data.put("speed", a.getSpeed());
        data.put("route", routePoints(a));
        return data;
    }

    private List<List<Double>> routePoints(Animal a) {
        List<List<Double>> points = new ArrayList<>();
        for (Position p : a.getRoute()) {
            List<Double> xy = new ArrayList<>();
            xy.add(p.getX());
            xy.add(p.getY());
            points.add(xy);
        }
        return points;
    }

    // Cafés sin tomar y banderas que están en la zona. 
    private List<Map<String, Object>> itemsIn(int zoneId) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (CoffeeCup cup : cups) {
            if (!cup.isTaken() && map.getZoneIdAt(cup.getPosition()) == zoneId) {
                items.add(itemData(cup));
            }
        }
        for (Player p : players()) {
            if (map.getZoneIdAt(p.getFlag().getPosition()) == zoneId) {
                items.add(itemData(p.getFlag()));
            }
        }
        return items;
    }

    private Map<String, Object> itemData(MapItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("x", item.getPosition().getX());
        data.put("y", item.getPosition().getY());
        data.put("image", item.getImageFile());
        return data;
    }

    // Las zonas donde el jugador tiene animales vivos (para el mapa pequeño). 
    private List<Integer> myZones(Player p) {
        List<Integer> zones = new ArrayList<>();
        for (Animal a : p.getPack().getAliveMembers()) {
            int zoneId = map.getZoneIdAt(a.getPosition());
            if (!zones.contains(zoneId)) {
                zones.add(zoneId);
            }
        }
        return zones;
    }

    // Fin de la partida 

    // Termina la partida, para el tick y manda GAME_OVER a los dos. 
    private void endGame(Player winner, String reason) {
        if (state == MatchState.FINISHED) {
            return;
        }
        state = MatchState.FINISHED;
        if (loop != null) {
            loop.shutdown();
        }
        Message msg = new Message(MessageType.GAME_OVER)
                .put("winner", winner.getNickname())
                .put("reason", reason)
                .put("seconds", elapsedSeconds())
                .put("players", playerSummaries())
                .put("bots", botSummaries());
        for (Player p : players()) {
            p.send(msg);
        }
    }

    /**
     * El resumen de cada jugador.
     * Si la partida termina en la selección, todavía no hay especie ni pack, por eso se revisa null.
     */
    private List<Map<String, Object>> playerSummaries() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Player p : players()) {
            Map<String, Object> data = new HashMap<>();
            data.put("nickname", p.getNickname());
            data.put("species", p.getSpecies() == null ? "" : p.getSpecies().name());
            data.put("botsKilled", p.getBotsKilled());
            data.put("cups", p.getCupsTaken());
            data.put("deadAnimals", p.getPack() == null ? 0 : p.getPack().countDead());
            list.add(data);
        }
        return list;
    }

    private List<Map<String, Object>> botSummaries() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Pack pack : packs) {
            if (pack.isBot()) {
                Map<String, Object> data = new HashMap<>();
                data.put("species", pack.getSpecies().name());
                data.put("deadAnimals", pack.countDead());
                list.add(data);
            }
        }
        return list;
    }

    // Ayudantes 

    // La ruta por los caminos desde el nodo más cercano al animal hasta el nodo más cercano al destino. 
    private List<Position> routeTo(Animal a, Position target) {
        return routeToNode(a, map.findNearestNode(target));
    }

    // La ruta por los caminos desde el nodo más cercano al animal hasta el nodo "end". 
    private List<Position> routeToNode(Animal a, PathNode end) {
        PathNode start = map.findNearestNode(a.getPosition());
        return PathFinder.findRoute(start, end);
    }

    // Segundos enteros desde que empezó la partida.
    private int elapsedSeconds() {
        return (int) ((lastTickTime - startTime) / 1000);
    }

    // Un animal vivo del pack del jugador, o null si no existe o ya murió. 
    private PlayerAnimal findOwnAnimal(Player p, int id) {
        Animal a = p.getPack().findAnimal(id);
        if (a == null || !a.isAlive()) {
            return null;
        }
        return (PlayerAnimal) a;
    }

    // Un animal vivo de un pack rival. 
    private Animal findEnemyAnimal(Player p, int id) throws GameException {
        for (Pack pack : packs) {
            if (pack == p.getPack()) {
                continue;
            }
            Animal a = pack.findAnimal(id);
            if (a != null && a.isAlive()) {
                return a;
            }
        }
        throw new GameException(GameConstants.ERROR_BAD_MESSAGE);
    }

    private boolean touches(MapItem item, Pack pack) {
        for (Animal a : pack.getAliveMembers()) {
            if (item.isTouchedBy(a)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOwnAnimalIn(Player p, int zoneId) {
        for (Animal a : p.getPack().getAliveMembers()) {
            if (map.getZoneIdAt(a.getPosition()) == zoneId) {
                return true;
            }
        }
        return false;
    }

    private List<Animal> allAnimals() {
        List<Animal> all = new ArrayList<>();
        for (Pack pack : packs) {
            all.addAll(pack.getMembers());
        }
        return all;
    }

    private List<Integer> idsOf(List<Animal> animals) {
        List<Integer> ids = new ArrayList<>();
        for (Animal a : animals) {
            ids.add(a.getId());
        }
        return ids;
    }

    private List<Player> players() {
        return Arrays.asList(player1, player2);
    }

    private Player otherPlayer(Player p) {
        return p == player1 ? player2 : player1;
    }

    private Position zoneCenter(int zoneId) {
        return map.getZone(zoneId).getCenter().getPosition();
    }

    private void checkPlaying() throws GameException {
        if (state != MatchState.PLAYING) {
            throw new GameException(GameConstants.ERROR_NOT_PLAYING);
        }
    }

    // Getters 

    public synchronized String getCode() {
        return code;
    }

    public synchronized MatchState getState() {
        return state;
    }

    // Una copia de la lista de packs (2 de jugadores y 5 bots). 
    public synchronized List<Pack> getPacks() {
        return new ArrayList<>(packs);
    }

    // Una copia de la lista de cafés. 
    public synchronized List<CoffeeCup> getCups() {
        return new ArrayList<>(cups);
    }
}
