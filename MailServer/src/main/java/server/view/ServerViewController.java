package server.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import server.model.ServerModel;
import server.network.SocketServer;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller della vista del server.
 * Gestisce l'interfaccia utente e l'avvio/arresto del server socket.
 */
public class ServerViewController implements Initializable {
    // ListView per visualizzare i log del server nell'interfaccia grafica
    @FXML private ListView<String> logListView;

    // Modello che gestisce i dati e i log del server
    private ServerModel model;
    // Istanza del server socket
    private SocketServer socketServer;

    /**
     * Inizializza il controller e avvia il server socket.
     * @param location URL della risorsa FXML
     * @param resources Risorse internazionalizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Crea il modello del server
        model = new ServerModel();
        // Collega la lista dei log al ListView
        logListView.setItems(model.getServerLog());

        // Avvia il server socket in un thread separato
        socketServer = new SocketServer(8080, model);
        Thread serverThread = new Thread(socketServer);
        serverThread.setDaemon(true); // Il thread si chiude con l'applicazione
        serverThread.start();

        // Aggiunge un messaggio di log all'avvio
        model.addToLog("Server avviato sulla porta 8080");
    }

    /**
     * Arresta il server socket e aggiunge un messaggio di log.
     */
    public void shutdown() {
        if (socketServer != null) {
            socketServer.stop();
            model.addToLog("Server arrestato");
        }
    }
}