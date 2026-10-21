package com.jungleattack.server.protocol;

/**
 * Los 13 tipos de mensaje del protocolo (sección 3 del spec).
 */
public enum MessageType {
    // Cliente -> Servidor
    CREATE_MATCH,
    JOIN_MATCH,
    SELECT_ANIMAL,
    MOVE_ANIMALS,
    ATTACK,
    VIEW_ZONE,

    // Servidor -> Cliente
    MATCH_CREATED,
    MATCH_JOINED,
    ANIMAL_SELECTION,
    GAME_STARTED,
    ZONE_UPDATE,
    GAME_OVER,
    ERROR
}
