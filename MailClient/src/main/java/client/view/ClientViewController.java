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
 * <p>
 * Responsabilità principali:
 * - orchestrare l'autenticazione e la gestione della sessione utente;
 * - collegare il {@link client.controller.ClientController} al modello e alla UI;
 * - configurare e sincronizzare le ListView (Posta in arrivo / Inviata);
 * - gestire le azioni dell'utente (login, compose, reply, forward, delete, refresh, logout);
 * - aggiornare periodicamente lo stato di connessione in UI senza bloccare il thread JavaFX.
 * </p>
 * Tutte le operazioni potenzialmente lunghe sono eseguite off-UI thread; gli aggiornamenti UI avvengono via
 * {@link javafx.application.Platform#runLater(Runnable)}.
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
        // Inizializza/Resetta lo stato dell'intera sessione all'apertura della view
        resetSession();
    }

    /**
     * Reinizializza la sessione: chiude eventuali risorse precedenti, ricrea controller e model, e prepara la UI.
     * Non blocca il thread JavaFX: l'inizializzazione della UI è delegata a Platform.runLater.
     */
    private void resetSession() {
        if (connectionStatusUpdater != null) connectionStatusUpdater.shutdownNow();
        if (controller != null) controller.shutdown();

        controller = new ClientController();
        model = controller.getModel();

        Platform.runLater(() -> {
            Stage stage = (Stage) emailField.getScene().getWindow();
            uiManager = new UIManager(stage, authBox, toolbarBox, mainSplitPane, connectionStatus, senderLabel, recipientsLabel, subjectLabel, dateLabel, bodyTextArea, replyButton, replyAllButton, forwardButton, deleteButton);
            // Mostra la schermata di login come stato iniziale
            uiManager.showLoginScreen();

            inboxListView.setItems(model.getInbox());
            sentListView.setItems(model.getSentEmails());

            setupListViews();               // Configura celle, listener e sincronizzazione selezioni
            startConnectionStatusUpdater(); // Aggiornamento periodico dello stato di connessione

            emailField.requestFocus();
        });
    }

    /**
     * Configura le ListView di posta in arrivo e inviata, compresa la factory delle celle e i listener
     * per mantenere selezioni mutualmente esclusive tra le due liste.
     */
    private void setupListViews() {
        EmailListViewConfigurator.configure(inboxListView, false, this::handleEmailSelection);
        EmailListViewConfigurator.configure(sentListView, true, this::handleEmailSelection);

        inboxListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
        // Se seleziono nella Inbox, svuoto la selezione della Sent
            if (newV != null) sentListView.getSelectionModel().clearSelection();
        });
        sentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
        // Se seleziono nella Sent, svuoto la selezione della Inbox
            if (newV != null) inboxListView.getSelectionModel().clearSelection();
        });
    }

    /**
     * Avvia un task schedulato che aggiorna periodicamente (ogni 2 secondi) lo stato di connessione
     * visualizzato nella UI. L'aggiornamento del label avviene sul thread JavaFX.
     */
    private void startConnectionStatusUpdater() {
        connectionStatusUpdater = Executors.newSingleThreadScheduledExecutor();
        connectionStatusUpdater.scheduleAtFixedRate(() ->
                Platform.runLater(() -> uiManager.updateConnectionStatus(model.isConnected())),
                0, 2, TimeUnit.SECONDS);
    }

    @FXML
    /**
     * Gestisce il click sul pulsante di login: valida il formato email e richiede autenticazione asincrona.
     * In caso di successo mostra l'interfaccia principale, altrimenti segnala errore e mantiene il focus sul campo.
     */
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

    /**
     * Callback di selezione email dalla ListView: aggiorna i dettagli e abilita le azioni contestuali.
     * @param email email selezionata
     */
    private void handleEmailSelection(Email email) {
        this.selectedEmail = email;
        uiManager.displayEmailDetails(email);
        boolean isSent = sentListView.getSelectionModel().getSelectedItem() != null;
        uiManager.enableEmailActions(true, isSent);
    }

    @FXML
    /**
     * Gestione eliminazione email: chiede conferma e, se positivo, invoca la cancellazione asincrona
     * lato controller. In caso di esito positivo, pulisce i dettagli e disabilita le azioni.
     */
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
    /**
     * Logout: arresta le risorse e ripristina lo stato iniziale dell'applicazione.
     */
    private void handleLogout() {
        shutdown();
        resetSession();
    }

    @FXML
    /**
     * Apre la finestra di composizione per una nuova email.
     */
    private void handleCompose() {
        openComposeWindow(null, false, false);
    }

    @FXML
    /**
     * Apre la finestra di composizione predisponendo una risposta al mittente.
     */
    private void handleReply() {
        if (selectedEmail != null) openComposeWindow(selectedEmail, true, false);
    }

    @FXML
    /**
     * Apre la finestra di composizione predisponendo una risposta a tutti i destinatari (escluso l'utente corrente).
     */
    private void handleReplyAll() {
        if (selectedEmail != null) openComposeWindow(selectedEmail, true, true);
    }

    @FXML
    /**
     * Apre la finestra di composizione predisponendo l'inoltro dell'email selezionata.
     */
    private void handleForward() {
        if (selectedEmail != null) openComposeWindow(selectedEmail, false, false);
    }

    @FXML
    /**
     * Forza una sincronizzazione manuale con il server se connessi. Eseguito su thread separato.
     */
    private void handleRefresh() {
        // Forza una sincronizzazione manuale con il server
        if (model != null && model.isConnected()) {
            new Thread(() -> model.syncWithServer()).start();
        }
    }

    /**
     * Apre la finestra di composizione e, se necessario, precompila campi per reply/forward.
     * @param email       email di riferimento (null per nuova email)
     * @param isReply     true per risposta
     * @param isReplyAll  true per risposta a tutti (valido solo se isReply è true)
     */
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

    /**
     * Arresta risorse in background (scheduler) e chiude la connessione del controller.
     * Da invocare in chiusura o al logout.
     */
    public void shutdown() {
        if (connectionStatusUpdater != null) connectionStatusUpdater.shutdownNow();
        if (controller != null) controller.shutdown();
    }
}
