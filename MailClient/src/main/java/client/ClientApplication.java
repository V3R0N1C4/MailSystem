package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import client.view.ClientViewController; // Import aggiuntivo

public class ClientApplication extends Application {
    private ClientViewController viewController; // Modifica il tipo

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ClientView.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        // Ottieni il view controller corretto
        viewController = loader.getController();

        primaryStage.setTitle("Mail Client");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (viewController != null) {  // Usa viewController qui
                viewController.shutdown();
            }
        });
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}