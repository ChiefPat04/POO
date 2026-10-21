package com.jungleattack.server.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.jungleattack.server.util.Position;

/**
 * Busca rutas en el grafo de caminos con BFS (búsqueda en anchura).
 * BFS encuentra la ruta con menos saltos.
 */
public final class PathFinder {

    private PathFinder() {
    }

    /**
     * @return la lista de puntos desde start hasta end (los dos incluidos).
     *         Si no hay camino, devuelve una lista vacía.
     */
    public static List<Position> findRoute(PathNode start, PathNode end) {
        Queue<PathNode> queue = new LinkedList<>();
        Set<PathNode> visited = new HashSet<>();
        Map<PathNode, PathNode> cameFrom = new HashMap<>();

        queue.add(start);
        visited.add(start);
        boolean found = false;

        while (!queue.isEmpty()) {
            PathNode current = queue.poll();
            if (current == end) {
                found = true;
                break;
            }
            for (PathNode neighbor : current.getNeighbors()) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    cameFrom.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        List<Position> route = new ArrayList<>();
        if (!found) {
            return route;
        }

        PathNode step = end;
        while (step != null) {
            route.add(new Position(step.getPosition()));
            step = cameFrom.get(step);
        }
        Collections.reverse(route);
        return route;
    }
}
