package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.view.ServerViewController;

/**
 * Classe principale dell'applicazione server.
 * Estende Application di JavaFX per gestire l'interfaccia grafica.
 */
public class ServerApplication extends Application {
    // Controller della vista del server, usato per gestire la logica della GUI e lo shutdown
    private ServerViewController controller;

    /**
     * Metodo di avvio dell'applicazione JavaFX.
     * Inizializza la finestra principale e collega il controller.
     *
     * @param primaryStage lo stage principale fornito da JavaFX
     * @throws Exception in caso di errori nel caricamento della GUI
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carica il file FXML della vista del server
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server/view/ServerView.fxml"));
        // Crea la scena con dimensioni 600x400
        Scene scene = new Scene(loader.load(), 600, 400);

        // Ottiene il controller associato alla vista
        controller = loader.getController();

        // Imposta il titolo della finestra
        primaryStage.setTitle("Mail Server");
        // Imposta la scena sulla finestra principale
        primaryStage.setScene(scene);
        // Gestisce la chiusura della finestra per eseguire lo shutdown del controller
        primaryStage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.shutdown();
            }
        });
        // Mostra la finestra principale
        primaryStage.show();
    }

    /**
     * Metodo main, punto di ingresso dell'applicazione.
     * Lancia l'applicazione JavaFX.
     *
     * @param args argomenti da linea di comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}