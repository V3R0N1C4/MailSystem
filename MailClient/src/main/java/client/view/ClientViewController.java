package client.view;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import client.controller.ClientController;
import client.model.ClientModel;
import common.model.EmailValidator;
import common.model.Email;

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
    // Componenti autenticazione
    @FXML private HBox authBox;         // Box per la sezione di autenticazione
    @FXML private TextField emailField; // Campo per inserire l'email
    @FXML private Button loginButton;   // Bottone per il login

    // Componenti toolbar
    @FXML private HBox toolbarBox;          // Toolbar principale
    @FXML private Button composeButton;     // Bottone per comporre una nuova email
    @FXML private Button refreshButton;     // Bottone per aggiornare la lista email
    @FXML private Label connectionStatus;   // Stato della connessione
    @FXML private Button logoutButton;      // Bottone per il logout

    // Componenti principali
    @FXML private SplitPane mainSplitPane;          // SplitPane principale della UI
    @FXML private TabPane folderTabPane;            // TabPane per le cartelle (inbox, sent)
    @FXML private Tab inboxTab;                     // Tab per la posta in arrivo
    @FXML private Tab sentTab;                      // Tab per la posta inviata
    @FXML private ListView<Email> inboxListView;    // Lista email in arrivo
    @FXML private ListView<Email> sentListView;     // Lista email inviate

    // Componenti dettagli email
    @FXML private Button replyButton;       // Bottone per rispondere
    @FXML private Button replyAllButton;    // Bottone per rispondere a tutti
    @FXML private Button forwardButton;     // Bottone per inoltrare
    @FXML private Button deleteButton;      // Bottone per eliminare
    @FXML private Label senderLabel;        // Label mittente
    @FXML private Label recipientsLabel;    // Label destinatari
    @FXML private Label subjectLabel;       // Label oggetto
    @FXML private Label dateLabel;          // Label data
    @FXML private TextArea bodyTextArea;    // Area testo corpo email

    private ClientController controller;                // Controller logico
    private ClientModel model;                          // Modello dati
    private Email selectedEmail;                        // Email selezionata
    private ScheduledExecutorService uiUpdateScheduler; // Scheduler per aggiornamenti UI

    /**
     * Inizializza la vista e i listener.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListViewAndListeners();
        resetSession();
    }

    /**
     * Configura le ListView e i listener di selezione.
     */
    private void setupListViewAndListeners() {
        // Configura le ListView con celle personalizzate
        inboxListView.setCellFactory(listView -> new EmailListCell(false));
        sentListView.setCellFactory(listView -> new EmailListCell(true));

        // Listener selezione email in inbox
        inboxListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                sentListView.getSelectionModel().clearSelection();
                handleEmailSelection(newVal);
            } else if (sentListView.getSelectionModel().getSelectedItem() == null) {
                clearEmailDetails();
                enableEmailActions(false);
            }
        });

        // Listener selezione email in sent
        sentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                inboxListView.getSelectionModel().clearSelection();
                handleEmailSelection(newVal);
            } else if (inboxListView.getSelectionModel().getSelectedItem() == null) {
                clearEmailDetails();
                enableEmailActions(false);
            }
        });
    }

    /**
     * Resetta la sessione utente e la UI.
     */
    private void resetSession() {
        // 1. Ferma servizi esistenti
        if (uiUpdateScheduler != null) {
            uiUpdateScheduler.shutdown();
        }
        if (controller != null) {
            controller.shutdown();
        }

        // 2. Crea nuove istanze controller e modello
        controller = new ClientController();
        model = controller.getModel();

        // 3. Collega le liste al modello
        inboxListView.setItems(model.getInbox());
        sentListView.setItems(model.getSentEmails());

        // 4. Avvia aggiornamento stato connessione
        startConnectionStatusUpdater();

        // 5. Resetta stato UI
        clearEmailDetails();
        enableEmailActions(false);
        selectedEmail = null;

        // 6. Focus sul campo email
        Platform.runLater(() -> emailField.requestFocus());
    }

    /**
     * Avvia un task periodico per aggiornare lo stato della connessione.
     */
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

    /**
     * Gestisce il login dell'utente.
     */
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();

        // Controlla se il campo è vuoto
        if (email.isEmpty()) {
            showAlert("Errore", "Inserisci un indirizzo email", Alert.AlertType.ERROR);
            return;
        }

        // Valida il formato email
        if (!EmailValidator.isValidEmailFormat(email)) {
            showAlert("Errore", "Formato email non valido", Alert.AlertType.ERROR);
            emailField.clear();
            emailField.requestFocus();
            return;
        }

        // Mostra indicatore di caricamento
        loginButton.setDisable(true);
        loginButton.setText("Connessione...");

        // Autenticazione in thread separato
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

    /**
     * Mostra l'interfaccia principale dopo il login.
     */
    private void showMainInterface() {
        authBox.setVisible(false);
        authBox.setManaged(false);
        toolbarBox.setVisible(true);
        toolbarBox.setManaged(true);
        mainSplitPane.setVisible(true);
        mainSplitPane.setManaged(true);

        // Aggiorna il titolo della finestra
        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setTitle("Mail Client - " + model.getUserEmail());
    }

    /**
     * Gestisce la pressione del bottone "Componi".
     */
    @FXML
    private void handleCompose() {
        openComposeWindow(null, false, false);
    }

    /**
     * Gestisce la pressione del bottone "Aggiorna".
     * Il refresh manuale mostra solo un feedback, la sincronizzazione è automatica.
     */
    @FXML
    private void handleRefresh() {
        refreshButton.setDisable(true);
        refreshButton.setText("Aggiornamento...");

        Thread refreshThread = new Thread(() -> {
            try {
                Thread.sleep(1000); // Breve attesa per feedback utente
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

    /**
     * Gestisce la selezione di una email.
     * @param selected email selezionata
     */
    private void handleEmailSelection(Email selected) {
        selectedEmail = selected;
        displayEmailDetails(selected);
        enableEmailActions(true);
    }

    /**
     * Mostra i dettagli della email selezionata.
     * @param email email da mostrare
     */
    private void displayEmailDetails(Email email) {
        senderLabel.setText(email.getSender());
        recipientsLabel.setText(String.join(", ", email.getRecipients()));
        subjectLabel.setText(email.getSubject());
        dateLabel.setText(email.getFormattedTimestamp());
        bodyTextArea.setText(email.getBody());
    }

    /**
     * Abilita o disabilita i bottoni di azione sulle email.
     * @param enable true per abilitare, false per disabilitare
     */
    private void enableEmailActions(boolean enable) {
        // Disabilita azioni non disponibili per i messaggi inviati
        boolean isSentEmail = sentListView.getSelectionModel().getSelectedItem() != null;

        replyButton.setDisable(!enable || isSentEmail);
        replyAllButton.setDisable(!enable || isSentEmail);
        forwardButton.setDisable(!enable);
        deleteButton.setDisable(!enable);
    }

    /**
     * Gestisce la pressione del bottone "Rispondi".
     */
    @FXML
    private void handleReply() {
        if (selectedEmail != null) {
            openComposeWindow(selectedEmail, true, false);
        }
    }

    /**
     * Gestisce la pressione del bottone "Rispondi a tutti".
     */
    @FXML
    private void handleReplyAll() {
        if (selectedEmail != null) {
            openComposeWindow(selectedEmail, true, true);
        }
    }

    /**
     * Gestisce la pressione del bottone "Inoltra".
     */
    @FXML
    private void handleForward() {
        if (selectedEmail != null) {
            openComposeWindow(selectedEmail, false, false);
        }
    }

    /**
     * Gestisce la pressione del bottone "Elimina".
     */
    @FXML
    private void handleDelete() {
        if (selectedEmail != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Conferma eliminazione");
            alert.setHeaderText("Eliminare questa email?");
            alert.setContentText("L'operazione non può essere annullata.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    boolean isSentEmail = sentListView.getSelectionModel().getSelectedItem() != null;
                    boolean success = controller.deleteEmail(selectedEmail, isSentEmail);

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

    /**
     * Gestisce il logout dell'utente e resetta la UI.
     */
    @FXML
    private void handleLogout() {
        // 1. Ferma i servizi in background
        if (uiUpdateScheduler != null) {
            uiUpdateScheduler.shutdown();
        }
        if (controller != null) {
            controller.shutdown();
        }

        // 2. Pulisci le liste e i dettagli
        inboxListView.setItems(FXCollections.observableArrayList());
        sentListView.setItems(FXCollections.observableArrayList());
        clearEmailDetails();

        // 3. Mostra la schermata di login
        authBox.setVisible(true);
        authBox.setManaged(true);
        toolbarBox.setVisible(false);
        toolbarBox.setManaged(false);
        mainSplitPane.setVisible(false);
        mainSplitPane.setManaged(false);

        // 4. Resetta lo stato della connessione
        connectionStatus.setText("Non connesso");
        connectionStatus.setStyle("-fx-text-fill: red;");

        // 5. Resetta il titolo della finestra
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.setTitle("Mail Client");

        // 6. Focus sul campo email
        emailField.clear();
        emailField.requestFocus();

        // 7. Prepara una nuova sessione
        resetSession();
    }

    /**
     * Pulisce i dettagli della email dalla UI.
     */
    private void clearEmailDetails() {
        senderLabel.setText("");
        recipientsLabel.setText("");
        subjectLabel.setText("");
        dateLabel.setText("");
        bodyTextArea.setText("");
    }

    /**
     * Apre la finestra di composizione email.
     * @param replyTo email a cui rispondere/inoltrare (null per nuova)
     * @param isReply true se risposta, false altrimenti
     * @param isReplyAll true se risposta a tutti, false altrimenti
     */
    private void openComposeWindow(Email replyTo, boolean isReply, boolean isReplyAll) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/view/ComposeView.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);

            ComposeViewController composeController = loader.getController();
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
            e.printStackTrace();
        }
    }

    /**
     * Mostra un alert modale.
     * @param title titolo finestra
     * @param message messaggio
     * @param type tipo di alert
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Arresta la sessione corrente (usato per chiusura applicazione).
     */
    public void shutdown() {
        handleLogout();
    }

    /**
     * Classe interna per la visualizzazione personalizzata delle email nelle ListView.
     */
    private static class EmailListCell extends ListCell<Email> {
        private final boolean isSentFolder;

        /**
         * Costruttore.
         * @param isSentFolder true se la cella è per la cartella "inviati"
         */
        public EmailListCell(boolean isSentFolder) {
            this.isSentFolder = isSentFolder;
        }

        /**
         * Aggiorna la visualizzazione della cella.
         */
        @Override
        protected void updateItem(Email email, boolean empty) {
            super.updateItem(email, empty);

            if (empty || email == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox content = new VBox(2);
                Label subjectLabel = new Label();
                Label detailsLabel = new Label();

                if (isSentFolder) {
                    // Visualizzazione per messaggi inviati
                    subjectLabel.setText("A: " + String.join(", ", email.getRecipients()));
                    detailsLabel.setText("Oggetto: " + email.getSubject());
                } else {
                    // Visualizzazione per inbox
                    subjectLabel.setText(email.getSubject());
                    detailsLabel.setText("Da: " + email.getSender());
                }

                subjectLabel.setStyle("-fx-font-weight: bold;");
                detailsLabel.setStyle("-fx-font-size: 0.9em; -fx-text-fill: gray;");

                content.getChildren().addAll(
                        subjectLabel,
                        detailsLabel,
                        new Label(email.getFormattedTimestamp()) {{
                            setStyle("-fx-font-size: 0.8em; -fx-text-fill: gray;");
                        }}
                );
                setGraphic(content);
            }
        }
    }
}