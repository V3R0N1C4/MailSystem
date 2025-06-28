// SocketServer.java - Server socket
package server.network;

import server.controller.ClientHandler;
import server.model.ServerModel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer implements Runnable {
    private final int port;
    private final ServerModel model;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public SocketServer(int port, ServerModel model) {
        this.port = port;
        this.model = model;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            model.addToLog("Server in ascolto sulla porta " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    model.addToLog("Nuova connessione da: " + clientSocket.getInetAddress());

                    // Gestisce ogni client in un thread separato
                    ClientHandler handler = new ClientHandler(clientSocket, model);
                    Thread handlerThread = new Thread(handler);
                    handlerThread.start();

                } catch (IOException e) {
                    if (running) {
                        model.addToLog("Errore nell'accettare connessioni: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            model.addToLog("Errore nell'avvio del server: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Errore nella chiusura del server: " + e.getMessage());
        }
    }
}