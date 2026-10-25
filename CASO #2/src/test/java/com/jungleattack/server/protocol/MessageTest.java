package com.jungleattack.server.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void everyMessageTypeSurvivesTheRoundTrip() {
        for (MessageType type : MessageType.values()) {
            Message original = new Message(type).put("code", "A7K2").put("number", 5);

            Message back = Message.fromJson(original.toJson());

            assertEquals(type, back.getType());
            assertEquals("A7K2", back.getString("code"));
            assertEquals(5, back.getInt("number"));
        }
    }

    @Test
    void listsSurviveTheRoundTrip() {
        Message original = new Message(MessageType.MOVE_ANIMALS).put("animalIds", List.of(3, 4, 7));

        Message back = Message.fromJson(original.toJson());

        assertEquals(List.of(3, 4, 7), back.getIntList("animalIds"));
    }

    @Test
    void decimalNumbersKeepTheirValue() {
        Message original = new Message(MessageType.MOVE_ANIMALS).put("targetX", 812.5).put("targetY", 344.25);

        Message back = Message.fromJson(original.toJson());

        assertEquals(812.5, back.getDouble("targetX"), 0.0001);
        assertEquals(344.25, back.getDouble("targetY"), 0.0001);
    }

    @Test
    void nestedObjectsSurviveTheRoundTrip() {
        // Así viene "bots" en GAME_OVER: una lista de objetos
        Message original = new Message(MessageType.GAME_OVER)
                .put("bots", List.of(Map.of("species", "TAPIR", "deadAnimals", 2)));

        Message back = Message.fromJson(original.toJson());

        assertEquals("TAPIR", back.getArray("bots").get(0).getAsJsonObject().get("species").getAsString());
        assertEquals(2, back.getArray("bots").get(0).getAsJsonObject().get("deadAnimals").getAsInt());
    }

    @Test
    void jsonIsOneLine() {
        String json = new Message(MessageType.ZONE_UPDATE).put("events", List.of("Tu pack tomó un café")).toJson();

        assertFalse(json.contains("\n"));
        assertTrue(json.startsWith("{\"type\":\"ZONE_UPDATE\""));
    }

    @Test
    void messageWithoutPayloadStillWorks() {
        Message msg = Message.fromJson("{\"type\":\"VIEW_ZONE\"}");

        assertEquals(MessageType.VIEW_ZONE, msg.getType());
        assertFalse(msg.has("zoneId"));
    }

    @Test
    void unknownTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Message.fromJson("{\"type\":\"NO_EXISTE\"}"));
    }
}
