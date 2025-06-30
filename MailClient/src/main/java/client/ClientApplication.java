package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import client.view.ClientViewController;

/**
 * Classe principale dell'applicazione client che avvia l'interfaccia grafica.
 */
public class ClientApplication extends Application {
    // Controller della vista principale
    private ClientViewController viewController;

    /**
     * Metodo di avvio dell'applicazione JavaFX.
     * @param primaryStage la finestra principale dell'applicazione
     * @throws Exception in caso di errore nel caricamento della vista
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carica il file FXML della vista principale
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ClientView.fxml"));
        // Crea la scena con dimensioni 800x600
        Scene scene = new Scene(loader.load(), 800, 600);

        // Ottiene il controller associato alla vista
        viewController = loader.getController();

        // Imposta il titolo della finestra
        primaryStage.setTitle("Mail Client");
        // Imposta la scena sulla finestra principale
        primaryStage.setScene(scene);

        // Gestisce la chiusura della finestra per terminare correttamente l'applicazione
        primaryStage.setOnCloseRequest(e -> {
            // Se il controller è presente, esegue le operazioni di shutdown
            if (viewController != null) {
                viewController.shutdown();
            }
            // Termina l'applicazione JavaFX
            Platform.exit();
            // Assicura la terminazione completa del processo
            System.exit(0);
        });

        // Mostra la finestra principale
        primaryStage.show();
    }

    /**
     * Metodo main, punto di ingresso dell'applicazione.
     * @param args argomenti da linea di comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}