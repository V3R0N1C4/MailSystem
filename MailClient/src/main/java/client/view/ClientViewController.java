package client.view;

import client.controller.ClientController;
import client.model.ClientModel;
import client.model.Email;
import client.model.EmailValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.SplitPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller della vista principale del client di posta.
 * Gestisce l'interazione tra l'utente e il modello, aggiornando la UI e rispondendo agli eventi.
 */
public class ClientViewController implements Initializable {

    //<editor-fold desc="FXML Fields">
    @FXML private HBox authBox;
    @FXML private TextField emailField;
    @FXML private Button loginButton;
    @FXML private HBox toolbarBox;
    @FXML private Button composeButton;
    @FXML private Button refreshButton;
    @FXML private Label connectionStatus;
    @FXML private Button logoutButton;
    @FXML private SplitPane mainSplitPane;
    @FXML private ListView<Email> inboxListView;
    @FXML private ListView<Email> sentListView;
    @FXML private Button replyButton;
    @FXML private Button replyAllButton;
    @FXML private Button forwardButton;
    @FXML private Button deleteButton;
    @FXML private Label senderLabel;
    @FXML private Label recipientsLabel;
    @FXML private Label subjectLabel;
    @FXML private Label dateLabel;
    @FXML private TextArea bodyTextArea;
    //</editor-fold>

    private ClientController controller;
    private ClientModel model;
    private UIManager uiManager;
    private Email selectedEmail;
    private ScheduledExecutorService connectionStatusUpdater;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        resetSession();
    }

    private void resetSession() {
        if (connectionStatusUpdater != null) connectionStatusUpdater.shutdownNow();
        if (controller != null) controller.shutdown();

        controller = new ClientController();
        model = controller.getModel();

        Platform.runLater(() -> {
            Stage stage = (Stage) emailField.getScene().getWindow();
            uiManager = new UIManager(stage, authBox, toolbarBox, mainSplitPane, connectionStatus, senderLabel, recipientsLabel, subjectLabel, dateLabel, bodyTextArea, replyButton, replyAllButton, forwardButton, deleteButton);
            uiManager.showLoginScreen();

            inboxListView.setItems(model.getInbox());
            sentListView.setItems(model.getSentEmails());

            setupListViews();
            startConnectionStatusUpdater();

            emailField.requestFocus();
        });
    }

    private void setupListViews() {
        EmailListViewConfigurator.configure(inboxListView, false, this::handleEmailSelection);
        EmailListViewConfigurator.configure(sentListView, true, this::handleEmailSelection);

        inboxListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) sentListView.getSelectionModel().clearSelection();
        });
        sentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) inboxListView.getSelectionModel().clearSelection();
        });
    }

    private void startConnectionStatusUpdater() {
        connectionStatusUpdater = Executors.newSingleThreadScheduledExecutor();
        connectionStatusUpdater.scheduleAtFixedRate(() ->
                Platform.runLater(() -> uiManager.updateConnectionStatus(model.isConnected())),
                0, 2, TimeUnit.SECONDS);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            DialogManager.showAlert("Errore", "Inserisci un indirizzo email", Alert.AlertType.ERROR);
            return;
        }
        if (!EmailValidator.isValidEmailFormat(email)) {
            DialogManager.showAlert("Errore", "Formato email non valido", Alert.AlertType.ERROR);
            return;
        }

        uiManager.setLoadingState(loginButton, true, "Connessione...", "Accedi");
        controller.authenticateUserAsync(email, success -> {
            uiManager.setLoadingState(loginButton, false, "Connessione...", "Accedi");
            if (success) {
                uiManager.showMainInterface(model.getUserEmail());
            } else {
                DialogManager.showAlert("Errore", "Email non esistente sul server", Alert.AlertType.ERROR);
                emailField.clear();
                emailField.requestFocus();
            }
        });
    }

    private void handleEmailSelection(Email email) {
        this.selectedEmail = email;
        uiManager.displayEmailDetails(email);
        boolean isSent = sentListView.getSelectionModel().getSelectedItem() != null;
        uiManager.enableEmailActions(true, isSent);
    }

    @FXML
    private void handleDelete() {
        if (selectedEmail == null) return;

        DialogManager.showConfirmation("Conferma eliminazione", "Eliminare questa email?", "L'operazione non può essere annullata.")
                .filter(response -> response == ButtonType.OK)
                .ifPresent(response -> {
                    boolean isSent = sentListView.getSelectionModel().getSelectedItem() != null;
                    controller.deleteEmailAsync(selectedEmail, isSent, success -> {
                        if (success) {
                            uiManager.clearEmailDetails();
                            uiManager.enableEmailActions(false, false);
                            selectedEmail = null;
                        } else {
                            DialogManager.showAlert("Errore", "Impossibile eliminare l'email", Alert.AlertType.ERROR);
                        }
                    });
                });
    }

    @FXML
    private void handleLogout() {
        shutdown();
        resetSession();
    }

    @FXML
    private void handleCompose() {
        openComposeWindow(null, false, false);
    }

    @FXML
    private void handleReply() {
        if (selectedEmail != null) openComposeWindow(selectedEmail, true, false);
    }

    @FXML
    private void handleReplyAll() {
        if (selectedEmail != null) openComposeWindow(selectedEmail, true, true);
    }

    @FXML
    private void handleForward() {
        if (selectedEmail != null) openComposeWindow(selectedEmail, false, false);
    }

    @FXML
    private void handleRefresh() {
        // Forza una sincronizzazione manuale con il server
        if (model != null && model.isConnected()) {
            new Thread(() -> model.syncWithServer()).start();
        }
    }

    private void openComposeWindow(Email email, boolean isReply, boolean isReplyAll) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ComposeView.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);

            ComposeViewController composeController = loader.getController();
            composeController.setClientController(controller);
            if (email != null) {
                composeController.setupReplyOrForward(email, isReply, isReplyAll);
            }

            Stage composeStage = new Stage();
            composeStage.setTitle(email == null ? "Nuova Email" : (isReply ? "Rispondi" : "Inoltra"));
            composeStage.setScene(scene);
            composeStage.initModality(Modality.WINDOW_MODAL);
            composeStage.initOwner(emailField.getScene().getWindow());
            composeStage.show();
        } catch (IOException e) {
            DialogManager.showAlert("Errore", "Impossibile aprire la finestra di composizione: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (connectionStatusUpdater != null) connectionStatusUpdater.shutdownNow();
        if (controller != null) controller.shutdown();
    }
}
