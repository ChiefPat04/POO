package com.jungleattack.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.jungleattack.server.game.GameException;
import com.jungleattack.server.game.GameSession;
import com.jungleattack.server.game.MatchState;
import com.jungleattack.server.lobby.MatchManager;
import com.jungleattack.server.model.Player;
import com.jungleattack.server.model.Species;
import com.jungleattack.server.protocol.Message;
import com.jungleattack.server.protocol.MessageType;
import com.jungleattack.server.util.GameConstants;

/**
 * Atiende a un cliente en su propio hilo: lee sus mensajes y se los pasa a la partida.
 */
public class ClientHandler implements Runnable, MessageSender {

    private final Socket socket;     // null cuando se usa en los tests
    private final BufferedReader in; // de donde llegan los mensajes del cliente
    private final PrintWriter out;   // por donde se le mandan los mensajes al cliente
    private Player player;           // se crea cuando el cliente crea o une una partida
    private GameSession session;     // la partida en la que está el cliente

    // Lo usa GameServer: lee y escribe por el socket
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    // Lo usan los tests: lee y escribe en memoria, sin socket
    public ClientHandler(BufferedReader in, PrintWriter out) {
        this.socket = null;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            // El cliente se cortó de golpe. No hay nada más que hacer con él.
        } finally {
            disconnect();
        }
    }

    // Procesa una línea JSON: según su tipo, llama al método que corresponde. 
    public void handleLine(String line) {
        try {
            Message msg = Message.fromJson(line);
            switch (msg.getType()) {
                case CREATE_MATCH:
                    createMatch(msg);
                    break;
                case JOIN_MATCH:
                    joinMatch(msg);
                    break;
                case SELECT_ANIMAL:
                    selectAnimal(msg);
                    break;
                case MOVE_ANIMALS:
                    requireSession().moveAnimals(player, msg.getIntList("animalIds"),
                            msg.getDouble("targetX"), msg.getDouble("targetY"));
                    break;
                case ATTACK:
                    requireSession().orderAttack(player, msg.getIntList("animalIds"), msg.getInt("targetId"));
                    break;
                case VIEW_ZONE:
                    requireSession().changeViewedZone(player, msg.getInt("zoneId"));
                    break;
                default:
                    // Los mensajes del servidor no los puede mandar el cliente
                    throw new GameException(GameConstants.ERROR_BAD_MESSAGE);
            }
        } catch (GameException e) {
            sendError(e.getMessage());
        } catch (RuntimeException e) {
            // JSON roto, tipo desconocido o un dato que falta
            sendError(GameConstants.ERROR_BAD_MESSAGE);
        }
    }

    private void createMatch(Message msg) throws GameException {
        checkNotInMatch();
        player = new Player(msg.getString("nickname"), this);
        session = MatchManager.getInstance().createMatch(player);
        send(new Message(MessageType.MATCH_CREATED).put("code", session.getCode()));
    }

    private void joinMatch(Message msg) throws GameException {
        checkNotInMatch();
        Player joiner = new Player(msg.getString("nickname"), this);
        GameSession joined = MatchManager.getInstance().joinMatch(msg.getString("code"), joiner);
        player = joiner;
        session = joined;
        session.announceJoined();
        session.announceSelection();
    }

    private void selectAnimal(Message msg) throws GameException {
        GameSession current = requireSession();
        Species species = Species.valueOf(msg.getString("species"));
        // Con synchronized, si los dos escogen casi al mismo tiempo, solo uno de los dos arranca la partida
        synchronized (current) {
            current.selectSpecies(player, species);
            current.announceSelection();
            if (current.bothSelected()) {
                current.startGame(System.currentTimeMillis());
                current.startLoop();
            }
        }
    }

    private void checkNotInMatch() throws GameException {
        if (session != null) {
            throw new GameException(GameConstants.ERROR_BAD_MESSAGE);
        }
    }

    private GameSession requireSession() throws GameException {
        if (session == null) {
            throw new GameException(GameConstants.ERROR_NOT_PLAYING);
        }
        return session;
    }

    // Se llama cuando el cliente se va: avisa a la partida y la borra si ya no sirve. 
    private void disconnect() {
        if (session != null) {
            session.playerDisconnected(player);
            if (session.getState() == MatchState.FINISHED) {
                MatchManager.getInstance().removeMatch(session.getCode());
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ya estaba cerrado: no importa
            }
        }
    }

    private void sendError(String text) {
        send(new Message(MessageType.ERROR).put("message", text));
    }

    // Varios hilos pueden mandar mensajes a este cliente, así que cada línea se escribe completa. 
    @Override
    public synchronized void send(Message message) {
        out.println(message.toJson());
    }
}
