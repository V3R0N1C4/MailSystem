// ClientViewController.java - Controller principale del client
package client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import client.controller.ClientController;
import client.model.ClientModel;
import client.model.Email;
import client.model.EmailValidator;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientViewController implements Initializable {
    // Componenti autenticazione
    @FXML private HBox authBox;
    @FXML private TextField emailField;
    @FXML private Button loginButton;

    // Componenti toolbar
    @FXML private HBox toolbarBox;
    @FXML private Button composeButton;
    @FXML private Button refreshButton;
    @FXML private Label connectionStatus;

    // Componenti principali
    @FXML private SplitPane mainSplitPane;
    @FXML private ListView<Email> emailListView;

    // Componenti dettagli email
    @FXML private Button replyButton;
    @FXML private Button replyAllButton;
    @FXML private Button forwardButton;
    @FXML private Button deleteButton;
    @FXML private Label senderLabel;
    @FXML private Label recipientsLabel;
    @FXML private Label subjectLabel;
    @FXML private Label dateLabel;
    @FXML private TextArea bodyTextArea;

    private ClientController controller;
    private ClientModel model;
    private Email selectedEmail;
    private ScheduledExecutorService uiUpdateScheduler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        controller = new ClientController();
        model = controller.getModel();

        // Configura la ListView
        emailListView.setItems(model.getInbox());
        emailListView.setCellFactory(listView -> new EmailListCell());

        // Avvia l'aggiornamento dello stato della connessione
        startConnectionStatusUpdater();

        // Focus sul campo email
        Platform.runLater(() -> emailField.requestFocus());
    }

    private void startConnectionStatusUpdater() {
        uiUpdateScheduler = Executors.newSingleThreadScheduledExecutor();
        uiUpdateScheduler.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                boolean connected = model.isConnected();
                connectionStatus.setText(connected ? "Connesso" : "Non connesso");
                connectionStatus.setStyle(connected ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
            });
        }, 0, 2, TimeUnit.SECONDS);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showAlert("Errore", "Inserisci un indirizzo email", Alert.AlertType.ERROR);
            return;
        }

        if (!EmailValidator.isValidEmailFormat(email)) {
            showAlert("Errore", "Formato email non valido", Alert.AlertType.ERROR);
            emailField.clear();
            emailField.requestFocus();
            return;
        }

        // Show loading indicator
        loginButton.setDisable(true);
        loginButton.setText("Connessione...");

        // Authenticate in background thread
        Thread authThread = new Thread(() -> {
            boolean success = controller.authenticateUser(email);

            Platform.runLater(() -> {
                loginButton.setDisable(false);
                loginButton.setText("Accedi");

                if (success) {
                    showMainInterface();
                } else {
                    showAlert("Errore", "Email non esistente sul server", Alert.AlertType.ERROR);
                    emailField.clear();
                    emailField.requestFocus();
                }
            });
        });
        authThread.setDaemon(true);
        authThread.start();
    }

    private void showMainInterface() {
        authBox.setVisible(false);
        authBox.setManaged(false);
        toolbarBox.setVisible(true);
        toolbarBox.setManaged(true);
        mainSplitPane.setVisible(true);
        mainSplitPane.setManaged(true);

        // Update window title
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setTitle("Mail Client - " + model.getUserEmail());
    }

    @FXML
    private void handleCompose() {
        openComposeWindow(null, false, false);
    }

    @FXML
    private void handleRefresh() {
        // Manual refresh is not needed as the model auto-syncs
        // But we can show a brief feedback
        refreshButton.setDisable(true);
        refreshButton.setText("Aggiornamento...");

        Thread refreshThread = new Thread(() -> {
            try {
                Thread.sleep(1000); // Brief delay for user feedback
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Platform.runLater(() -> {
                refreshButton.setDisable(false);
                refreshButton.setText("Aggiorna");
            });
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    @FXML
    private void handleEmailSelection(MouseEvent event) {
        Email selected = emailListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedEmail = selected;
            displayEmailDetails(selected);
            enableEmailActions(true);
        }
    }

    private void displayEmailDetails(Email email) {
        senderLabel.setText(email.getSender());
        recipientsLabel.setText(String.join(", ", email.getRecipients()));
        subjectLabel.setText(email.getSubject());
        dateLabel.setText(email.getFormattedTimestamp());
        bodyTextArea.setText(email.getBody());
    }

    private void enableEmailActions(boolean enable) {
        replyButton.setDisable(!enable);
        replyAllButton.setDisable(!enable);
        forwardButton.setDisable(!enable);
        deleteButton.setDisable(!enable);
    }

    @FXML
    private void handleReply() {
        if (selectedEmail != null) {
            openComposeWindow(selectedEmail, true, false);
        }
    }

    @FXML
    private void handleReplyAll() {
        if (selectedEmail != null) {
            openComposeWindow(selectedEmail, true, true);
        }
    }

    @FXML
    private void handleForward() {
        if (selectedEmail != null) {
            openComposeWindow(selectedEmail, false, false);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedEmail != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Conferma eliminazione");
            alert.setHeaderText("Eliminare questa email?");
            alert.setContentText("L'operazione non può essere annullata.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    boolean success = controller.deleteEmail(selectedEmail);
                    if (success) {
                        clearEmailDetails();
                        enableEmailActions(false);
                        selectedEmail = null;
                    } else {
                        showAlert("Errore", "Impossibile eliminare l'email", Alert.AlertType.ERROR);
                    }
                }
            });
        }
    }

    private void clearEmailDetails() {
        senderLabel.setText("");
        recipientsLabel.setText("");
        subjectLabel.setText("");
        dateLabel.setText("");
        bodyTextArea.setText("");
    }

    private void openComposeWindow(Email replyTo, boolean isReply, boolean isReplyAll) {
        try {
            // CORRECTED FXML PATH
            String[] possiblePaths = {
                    "/client/view/ComposeView.fxml"  // This is the correct path
            };

            FXMLLoader loader = null;
            for (String path : possiblePaths) {
                if (getClass().getResource(path) != null) {
                    loader = new FXMLLoader(getClass().getResource(path));
                    break;
                }
            }

            Scene scene = new Scene(loader.load(), 600, 500);

            client.view.ComposeViewController composeController = loader.getController();
            composeController.setClientController(controller);

            if (replyTo != null) {
                composeController.setupReplyOrForward(replyTo, isReply, isReplyAll);
            }

            Stage composeStage = new Stage();
            composeStage.setTitle(replyTo == null ? "Nuova Email" :
                    (isReply ? "Rispondi" : "Inoltra"));
            composeStage.setScene(scene);
            composeStage.initModality(Modality.WINDOW_MODAL);
            composeStage.initOwner(emailField.getScene().getWindow());
            composeStage.show();

        } catch (IOException e) {
            showAlert("Errore", "Impossibile aprire la finestra di composizione: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace(); // This will help debug the issue
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void shutdown() {
        if (uiUpdateScheduler != null && !uiUpdateScheduler.isShutdown()) {
            uiUpdateScheduler.shutdown();
        }
        if (controller != null) {
            controller.shutdown();
        }
    }

    // Custom ListCell for Email display
    private static class EmailListCell extends ListCell<Email> {
        @Override
        protected void updateItem(Email email, boolean empty) {
            super.updateItem(email, empty);

            if (empty || email == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox content = new VBox(2);
                content.getChildren().addAll(
                        new Label(email.getSubject()) {{
                            setStyle("-fx-font-weight: bold;");
                        }},
                        new Label("Da: " + email.getSender()) {{
                            setStyle("-fx-font-size: 0.9em; -fx-text-fill: gray;");
                        }},
                        new Label(email.getFormattedTimestamp()) {{
                            setStyle("-fx-font-size: 0.8em; -fx-text-fill: gray;");
                        }}
                );
                setGraphic(content);
            }
        }
    }
}
