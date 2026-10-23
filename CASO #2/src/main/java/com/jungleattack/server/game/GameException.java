package com.jungleattack.server.game;

/**
 * Un error de las reglas del juego. Su mensaje se le manda al cliente en un ERROR.
 */
public class GameException extends Exception {

    public GameException(String message) {
        super(message);
    }
}
