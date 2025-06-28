// ServerApplication.java - Applicazione server
package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.controller.ServerController;

import java.io.FileNotFoundException;
import java.net.URL;

public class ServerApplication extends Application {
    private ServerController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Usa il percorso assoluto con slash iniziale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server/view/ServerView.fxml"));

        // Codice diagnostico
        URL fxmlUrl = getClass().getResource("/server/view/ServerView.fxml");
        if(fxmlUrl == null) {
            System.err.println("ERRORE: File non trovato! Cercato in: ");
            System.err.println("Classpath: " + System.getProperty("java.class.path"));
            throw new FileNotFoundException("ServerView.fxml non trovato");
        }
        System.out.println("Trovato FXML: " + fxmlUrl);

        Scene scene = new Scene(loader.load(), 600, 400);

        controller = loader.getController();

        primaryStage.setTitle("Mail Server");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.shutdown();
            }
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}