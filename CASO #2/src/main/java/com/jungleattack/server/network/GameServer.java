package com.jungleattack.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.jungleattack.server.util.GameConstants;


//El punto de entrada del servidor: abre el puerto y atiende a cada cliente que se conecta.
 
public class GameServer {

    public static void main(String[] args) throws IOException {
        // Un hilo por cliente. Los hilos se reutilizan cuando un cliente se va.
        ExecutorService clients = Executors.newCachedThreadPool();

        try (ServerSocket server = new ServerSocket(GameConstants.SERVER_PORT)) {
            System.out.println("Servidor escuchando en el puerto " + GameConstants.SERVER_PORT);
            while (true) {
                // accept() espera hasta que alguien se conecte, y devuelve su socket
                Socket socket = server.accept();
                try {
                    clients.execute(new ClientHandler(socket));
                } catch (IOException e) {
                    System.out.println("No se pudo atender a un cliente: " + e.getMessage());
                    socket.close();
                }
            }
        }
    }
}
