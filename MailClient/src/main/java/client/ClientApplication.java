// ClientApplication.java - Applicazione client
package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import client.controller.ClientController;

public class ClientApplication extends Application {
    private ClientController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ClientView.fxml"));
        Scene scene = new Scene(loader.load(), 800, 600);

        controller = loader.getController();

        primaryStage.setTitle("Mail Client");
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