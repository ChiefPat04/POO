package com.jungleattack.server.game;

/**
 * En qué etapa está una partida.
 */
public enum MatchState {
    WAITING_FOR_PLAYER,
    SELECTING_ANIMALS,
    PLAYING,
    FINISHED
}
