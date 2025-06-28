// ServerViewController.java - Controller per la vista del server
package server.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import server.model.ServerModel;
import server.network.SocketServer;
import java.net.URL;
import java.util.ResourceBundle;

public class ServerViewController implements Initializable {
    @FXML private ListView<String> logListView;

    private ServerModel model;
    private SocketServer socketServer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        model = new ServerModel();
        logListView.setItems(model.getServerLog());

        // Avvia il server socket in un thread separato
        socketServer = new SocketServer(8080, model);
        Thread serverThread = new Thread(socketServer);
        serverThread.setDaemon(true);
        serverThread.start();

        model.addToLog("Server avviato sulla porta 8080");
    }

    public void shutdown() {
        if (socketServer != null) {
            socketServer.stop();
            model.addToLog("Server arrestato");
        }
    }
}