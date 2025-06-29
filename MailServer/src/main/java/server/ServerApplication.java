// ServerApplication.java - Applicazione server
package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.view.ServerViewController;

public class ServerApplication extends Application {
    private ServerViewController controller; // Cambiato il tipo

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server/view/ServerView.fxml"));
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