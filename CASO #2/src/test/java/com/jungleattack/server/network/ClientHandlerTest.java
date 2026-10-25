package com.jungleattack.server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.jungleattack.server.protocol.Message;
import com.jungleattack.server.protocol.MessageType;
import com.jungleattack.server.util.GameConstants;

class ClientHandlerTest {

    @Test
    void createMatchAnswersWithTheCode() {
        String request = "{\"type\":\"CREATE_MATCH\",\"payload\":{\"nickname\":\"Ana\"}}\n";
        StringWriter written = new StringWriter();
        ClientHandler handler = new ClientHandler(
                new BufferedReader(new StringReader(request)),
                new PrintWriter(written, true));

        handler.run(); // lee la línea, responde y termina al llegar al final

        Message reply = Message.fromJson(written.toString().trim());
        assertEquals(MessageType.MATCH_CREATED, reply.getType());
        assertEquals(GameConstants.CODE_LENGTH, reply.getString("code").length());
    }

    @Test
    void brokenJsonAnswersWithAnError() {
        StringWriter written = new StringWriter();
        ClientHandler handler = new ClientHandler(
                new BufferedReader(new StringReader("")),
                new PrintWriter(written, true));

        handler.handleLine("esto no es JSON");

        Message reply = Message.fromJson(written.toString().trim());
        assertEquals(MessageType.ERROR, reply.getType());
        assertEquals(GameConstants.ERROR_BAD_MESSAGE, reply.getString("message"));
    }
}
