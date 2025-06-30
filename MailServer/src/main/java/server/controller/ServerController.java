package server.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import server.model.ServerModel;
import server.network.SocketServer;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller del server che gestisce l'interfaccia utente e l'avvio del server socket.
 */
public class ServerController implements Initializable {
    @FXML private ListView<String> logListView; // Riferimento alla ListView per visualizzare i log del server nell'interfaccia grafica
    private ServerModel model;                  // Modello che gestisce i dati e i log del server
    private SocketServer socketServer;          // Istanza del server socket che gestisce le connessioni di rete

    /**
     * Metodo chiamato automaticamente all'inizializzazione del controller.
     * Collega la ListView al modello e avvia il server socket.
     *
     * @param location  posizione del file FXML
     * @param resources risorse utilizzate dall'interfaccia
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializza il modello del server
        model = new ServerModel();

        // Collega la ListView ai log del modello
        logListView.setItems(model.getServerLog());

        // Crea e avvia il server socket sulla porta 8080, passandogli il modello
        socketServer = new SocketServer(8080, model);
        Thread serverThread = new Thread(socketServer);
        serverThread.setDaemon(true); // Imposta il thread come demone per chiusura automatica
        serverThread.start();

        // Aggiunge un messaggio di log all'avvio del server
        model.addToLog("Server avviato sulla porta 8080");
    }

    /**
     * Metodo per arrestare il server socket in modo sicuro.
     */
    public void shutdown() {
        if (socketServer != null) {
            socketServer.stop();
        }
    }
}