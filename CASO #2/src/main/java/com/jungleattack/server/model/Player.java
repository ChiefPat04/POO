package com.jungleattack.server.model;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.server.network.MessageSender;
import com.jungleattack.server.protocol.Message;
import com.jungleattack.server.util.GameConstants;

/**
 * Un jugador conectado: su nombre, su pack, su bandera y sus estadísticas.
 */
public class Player {

    private final String nickname;
    private final MessageSender sender;
    private Species species;
    private PlayerPack pack;
    private Flag flag;
    private int viewedZoneId;
    private List<Integer> lastVisibleIds;
    private int botsKilled;
    private int cupsTaken;
    private final List<String> pendingEvents;

    public Player(String nickname, MessageSender sender) {
        this.nickname = nickname;
        this.sender = sender;
        this.lastVisibleIds = new ArrayList<>();
        this.pendingEvents = new ArrayList<>();
    }

    /** Guarda un evento para mandarlo en el próximo ZONE_UPDATE. */
    public void addEvent(String text) {
        pendingEvents.add(text);
    }

    /** Devuelve los eventos pendientes y vacía la lista, para no mandarlos dos veces. */
    public List<String> takeEvents() {
        List<String> copy = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        return copy;
    }

    /**
     * Suma un bot muerto.
     * @return true si llegó a un múltiplo de BOTS_KILLED_FOR_BONUS (le toca el bono)
     */
    public boolean addBotKill() {
        botsKilled++;
        return botsKilled % GameConstants.BOTS_KILLED_FOR_BONUS == 0;
    }

    public void addCup() {
        cupsTaken++;
    }

    public void send(Message message) {
        if (sender != null) {
            sender.send(message);
        }
    }

    // Getters y setters 

    public String getNickname() {
        return nickname;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    public PlayerPack getPack() {
        return pack;
    }

    public void setPack(PlayerPack pack) {
        this.pack = pack;
    }

    public Flag getFlag() {
        return flag;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public int getViewedZoneId() {
        return viewedZoneId;
    }

    public void setViewedZoneId(int viewedZoneId) {
        this.viewedZoneId = viewedZoneId;
    }

    public List<Integer> getLastVisibleIds() {
        return lastVisibleIds;
    }

    public void setLastVisibleIds(List<Integer> lastVisibleIds) {
        this.lastVisibleIds = new ArrayList<>(lastVisibleIds); //Este guarda una copia de la lista, para que nadie la cambie desde afuera después.
    }

    public int getBotsKilled() {
        return botsKilled;
    }

    public int getCupsTaken() {
        return cupsTaken;
    }
}
