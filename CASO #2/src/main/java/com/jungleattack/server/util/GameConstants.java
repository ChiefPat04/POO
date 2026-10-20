package com.jungleattack.server.util;

/**
 * Todos los números y textos fijos del juego aquí en un solo lugar.
 * Así no hay números aleatorios regados por el código.
 */
public final class GameConstants {

    // Constructor privado: nadie puede hacer "new GameConstants()"
    private GameConstants() {
    }

    // Red 
    public static final int SERVER_PORT = 5000;

    // Código de partida
    public static final int CODE_LENGTH = 4;
    public static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    // Mapa 
    public static final int ZONE_COLUMNS = 4;
    public static final int ZONE_ROWS = 3;
    public static final int ZONE_COUNT = ZONE_COLUMNS * ZONE_ROWS;
    public static final int ZONE_WIDTH = 800;
    public static final int ZONE_HEIGHT = 600;
    public static final int DOOR_MARGIN = 40;

    // Pack del jugador 
    public static final int PLAYER_PACK_SIZE = 10;
    public static final int PLAYER_START_ENERGY = 20;
    public static final int MAX_PLAYER_ENERGY = 20;
    public static final int PLAYER_START_DAMAGE = 2;

    // Pack bot 
    public static final int BOT_PACK_COUNT = 5;
    public static final int BOT_PACK_SIZE = 4;
    public static final int BOT_START_ENERGY = 10;
    public static final int BOT_DAMAGE = 1;

    // Cafés y bono 
    public static final int COFFEE_CUP_COUNT = 10;
    public static final int COFFEE_DAMAGE_BONUS = 1;
    public static final int BOTS_KILLED_FOR_BONUS = 2;
    public static final int ENERGY_BONUS = 1;

    // Movimiento (píxeles por segundo) 
    public static final double ANIMAL_SPEED = 80;
    public static final double BOT_SPEED = 50;
    public static final double SPAWN_SPREAD = 30;

    //  Distancias 
    public static final double ENCOUNTER_RADIUS = 40;
    public static final double PICKUP_RADIUS = 20;
    public static final double FLAG_RADIUS = 30;

    //  Tiempos (milisegundos) 
    public static final long TICK_MS = 50;
    public static final long ATTACK_COOLDOWN_MS = 1000;
    public static final long BOT_WAIT_MS = 3000;
    public static final long SYNC_MS = 1000;

    //  Imágenes 
    public static final String COFFEE_IMAGE = "coffee.svg";
    public static final String FLAG_IMAGE_PLAYER1 = "flag_red.svg";
    public static final String FLAG_IMAGE_PLAYER2 = "flag_blue.svg";

    // Mensajes de error 
    public static final String ERROR_BAD_CODE = "No existe una partida con ese código";
    public static final String ERROR_MATCH_FULL = "La partida ya está llena";
    public static final String ERROR_SPECIES_TAKEN = "Esa especie ya la escogió el otro jugador";
    public static final String ERROR_NOT_PLAYING = "La partida no está en juego";
    public static final String ERROR_BAD_MESSAGE = "Mensaje no válido";
}
