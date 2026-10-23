package com.jungleattack.server.model;

import java.util.List;
import java.util.Random;

import com.jungleattack.server.map.GameMap;
import com.jungleattack.server.map.PathFinder;
import com.jungleattack.server.map.PathNode;
import com.jungleattack.server.util.GameConstants;
import com.jungleattack.server.util.Position;

/**
 * Un pack bot: 4 animales con daño 1 que patrullan su propia zona.
 */
public class BotPack extends Pack {

    private final int homeZoneId;
    private long nextMoveTime;

    public BotPack(Species species, int homeZoneId) {
        super(species, GameConstants.BOT_DAMAGE);
        this.homeZoneId = homeZoneId;
        this.nextMoveTime = 0;
    }

    @Override
    protected Animal createAnimal(int id, Position position) {
        return new BotAnimal(id, this, position);
    }

    @Override
    public int getSize() {
        return GameConstants.BOT_PACK_SIZE;
    }

    @Override
    public boolean isBot() {
        return true;
    }

    /**
     * Patrulla: si todos están quietos y ya pasó la espera,
     * escoge un nodo al azar de su zona y manda a todo el pack hacia ahí.
     *
     * @return true si el pack empezó a caminar en esta llamada
     */
    public boolean updatePatrol(GameMap map, Random random, long now) {
        List<Animal> alive = getAliveMembers();

        for (Animal a : alive) {
            if (a.isMoving()) {
                nextMoveTime = now + GameConstants.BOT_WAIT_MS;
                return false;
            }
        }
        if (now < nextMoveTime || alive.isEmpty()) {
            return false;
        }

        List<PathNode> zoneNodes = map.getZone(homeZoneId).getNodes();
        PathNode destination = zoneNodes.get(random.nextInt(zoneNodes.size()));

        for (Animal a : alive) {
            PathNode start = map.findNearestNode(a.getPosition());
            a.setRoute(PathFinder.findRoute(start, destination));
        }
        return true;
    }

    /**
     * El pack espera BOT_WAIT_MS antes de volver a patrullar.
     * Se llama mientras alguno de sus bots está peleando.
     */
    public void pauseFor(long now) {
        nextMoveTime = now + GameConstants.BOT_WAIT_MS;
    }

    public int getHomeZoneId() {
        return homeZoneId;
    }

    public long getNextMoveTime() {
        return nextMoveTime;
    }
}
