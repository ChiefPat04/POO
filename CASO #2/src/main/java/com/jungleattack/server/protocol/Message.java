package com.jungleattack.server.protocol;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * El "sobre" de todos los mensajes: un tipo y un payload con los datos.
 * Ejemplo: new Message(MessageType.MATCH_CREATED).put("code", "A7K2")
 */
public class Message {

    private static final Gson gson = new Gson();

    private final MessageType type;
    private final JsonObject payload;

    public Message(MessageType type) {
        this(type, new JsonObject());
    }

    private Message(MessageType type, JsonObject payload) {
        this.type = type;
        this.payload = payload;
    }

    /** Agrega un dato (número, texto, booleano o lista). Devuelve "this" para encadenar. */
    public Message put(String key, Object value) {
        if (value instanceof Number) {
            payload.addProperty(key, (Number) value);
        } else if (value instanceof String) {
            payload.addProperty(key, (String) value);
        } else if (value instanceof Boolean) {
            payload.addProperty(key, (Boolean) value);
        } else {
            // Listas, arreglos u objetos: Gson los convierte solo
            payload.add(key, gson.toJsonTree(value));
        }
        return this;
    }

    public boolean has(String key) {
        return payload.has(key);
    }

    public int getInt(String key) {
        return payload.get(key).getAsInt();
    }

    public double getDouble(String key) {
        return payload.get(key).getAsDouble();
    }

    public String getString(String key) {
        return payload.get(key).getAsString();
    }

    public JsonArray getArray(String key) {
        return payload.get(key).getAsJsonArray();
    }

    public List<Integer> getIntList(String key) {
        List<Integer> list = new ArrayList<>();
        for (JsonElement e : getArray(key)) {
            list.add(e.getAsInt());
        }
        return list;
    }

    public MessageType getType() {
        return type;
    }

    public JsonObject getPayload() {
        return payload;
    }

    /** El mensaje como JSON en una sola línea. */
    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("type", type.name());
        root.add("payload", payload);
        return gson.toJson(root);
    }

    /** Convierte una línea JSON en un Message. */
    public static Message fromJson(String line) {
        JsonObject root = JsonParser.parseString(line).getAsJsonObject();
        MessageType type = MessageType.valueOf(root.get("type").getAsString());
        JsonObject payload;
        if (root.has("payload")) {
            payload = root.getAsJsonObject("payload");
        } else {
            payload = new JsonObject();
        }
        return new Message(type, payload);
    }
}
