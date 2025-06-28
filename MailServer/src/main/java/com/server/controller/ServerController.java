// ServerController.java - Controller principale del server
package server.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import server.model.ServerModel;
import server.network.SocketServer;
import java.net.URL;
import java.util.ResourceBundle;

public class ServerController implements Initializable {
    @FXML private ListView<String> logListView;

    private ServerModel model;
    private SocketServer socketServer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        model = new ServerModel();

        // Collega la ListView al modello
        logListView.setItems(model.getServerLog());

        // Avvia il server socket
        socketServer = new SocketServer(8080, model);
        Thread serverThread = new Thread(socketServer);
        serverThread.setDaemon(true);
        serverThread.start();

        model.addToLog("Server avviato sulla porta 8080");
    }

    public void shutdown() {
        if (socketServer != null) {
            socketServer.stop();
        }
    }
}