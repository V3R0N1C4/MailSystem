package server.network;

import server.controller.ClientHandler;
import server.model.ServerModel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe che rappresenta un server socket che accetta connessioni dai client.
 * Ogni client viene gestito in un thread separato tramite ClientHandler.
 */
public class SocketServer implements Runnable {
    private final int port;                     // Porta su cui il server ascolta le connessioni
    private final ServerModel model;            // Modello del server per la gestione della logica applicativa
    private ServerSocket serverSocket;          // Socket del server per accettare le connessioni
    private volatile boolean running = true;    // Flag per controllare lo stato di esecuzione del server

    /**
     * Costruttore della classe SocketServer.
     * @param port la porta su cui il server ascolta
     * @param model il modello del server
     */
    public SocketServer(int port, ServerModel model) {
        this.port = port;
        this.model = model;
    }

    /**
     * Metodo principale del thread: avvia il server e gestisce le connessioni in ingresso.
     */
    @Override
    public void run() {
        try {
            // Crea il ServerSocket sulla porta specificata
            serverSocket = new ServerSocket(port);
            model.addToLog("Server in ascolto sulla porta " + port);

            // Ciclo principale: accetta nuove connessioni finché il server è in esecuzione
            while (running) {
                try {
                    // Accetta una nuova connessione dal client
                    Socket clientSocket = serverSocket.accept();
                    model.addToLog("Nuova connessione da: " + clientSocket.getInetAddress());

                    // Gestisce ogni client in un thread separato
                    ClientHandler handler = new ClientHandler(clientSocket, model);
                    Thread handlerThread = new Thread(handler);
                    handlerThread.start();

                } catch (IOException e) {
                    // Gestisce eventuali errori durante l'accettazione delle connessioni
                    if (running) {
                        model.addToLog("Errore nell'accettare connessioni: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            // Gestisce errori nell'avvio del server
            model.addToLog("Errore nell'avvio del server: " + e.getMessage());
        }
    }

    /**
     * Ferma il server chiudendo il ServerSocket e impostando il flag running a false.
     */
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