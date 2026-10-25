package com.jungleattack.server.lobby;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.jungleattack.server.game.GameException;
import com.jungleattack.server.game.GameSession;
import com.jungleattack.server.model.Player;
import com.jungleattack.server.util.GameConstants;

/**
 * Guarda todas las partidas activas del servidor.
 * Es un Singleton: solo debe existir un administrador de partidas.
 */
public class MatchManager {

    // La única instancia. Se crea una vez, cuando se carga la clase.
    private static final MatchManager INSTANCE = new MatchManager();

    private final ConcurrentHashMap<String, GameSession> sessions; // código -> partida
    private final Random random;

    // Constructor privado: nadie puede hacer "new MatchManager()" desde afuera.
    private MatchManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    public static MatchManager getInstance() {
        return INSTANCE;
    }

    // Crea una partida nueva con un código único y el jugador como creador.
    public synchronized GameSession createMatch(Player creator) {
        String code = generateCode();
        GameSession session = new GameSession(code, creator, random);
        sessions.put(code, session);
        return session;
    }

    /**
     * Mete al segundo jugador en la partida con ese código.
     * @throws GameException si el código no existe o la partida ya está llena
     */
    public GameSession joinMatch(String code, Player p) throws GameException {
        GameSession session = sessions.get(normalizeCode(code));
        if (session == null) {
            throw new GameException(GameConstants.ERROR_BAD_CODE);
        }
        session.addSecondPlayer(p); // este método revisa si ya está llena
        return session;
    }

    // Borra una partida. Se usa cuando termina o si el creador se va antes de que entre el otro. 
    public void removeMatch(String code) {
        sessions.remove(normalizeCode(code));
    }

    /**
     * Crea un código de CODE_LENGTH caracteres (letras y números).
     * Vuelve a sortear mientras el código ya esté en uso por una partida activa.
     */
    public synchronized String generateCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < GameConstants.CODE_LENGTH; i++) {
                int index = random.nextInt(GameConstants.CODE_CHARACTERS.length());
                sb.append(GameConstants.CODE_CHARACTERS.charAt(index));
            }
            code = sb.toString();
        } while (sessions.containsKey(code));
        return code;
    }

    // Pasa el código a mayúsculas y le quita los espacios, así "a7k2 " funciona igual que "A7K2".
    public static String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toUpperCase();
    }
}
