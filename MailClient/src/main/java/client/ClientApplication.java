// ClientApplication.java
package client;

import javafx.application.Application;
import javafx.application.Platform; // Import aggiunto
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import client.view.ClientViewController;

public class ClientApplication extends Application {
    private ClientViewController viewController;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ClientView.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        viewController = loader.getController();

        primaryStage.setTitle("Mail Client");
        primaryStage.setScene(scene);

        // Chiudi correttamente l'applicazione alla chiusura della finestra
        primaryStage.setOnCloseRequest(e -> {
            if (viewController != null) {
                viewController.shutdown();
            }
            Platform.exit(); // Termina l'applicazione JavaFX
            System.exit(0);  // Assicura la terminazione completa
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}