package com.jungleattack.server.network;

import com.jungleattack.server.protocol.Message;

/**
 * Algo a lo que se le puede mandar un mensaje.
 * Lo implementa ClientHandler. En los tests usamos uno falso que guarda los mensajes en una lista.
 */
public interface MessageSender {

    void send(Message message);
}
