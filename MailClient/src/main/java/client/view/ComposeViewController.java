package client.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import client.controller.ClientController;
import common.model.EmailValidator;
import common.model.Email;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller per la vista di composizione email.
 */
public class ComposeViewController implements Initializable {
    @FXML private TextField toField;        // Campo destinatari
    @FXML private TextField subjectField;   // Campo oggetto
    @FXML private TextArea bodyTextArea;    // Area di testo per il corpo dell'email
    @FXML private Button sendButton;        // Pulsante invio
    @FXML private Button cancelButton;      // Pulsante annulla

    private ClientController clientController;  // Controller principale dell'applicazione
    private Email originalEmail;                // Email originale (per rispondi/inoltra)
    private boolean isReply;                    // Flag per modalità risposta
    private boolean isReplyAll;                 // Flag per modalità risposta a tutti

    /**
     * Inizializza la vista e imposta la validazione dei campi.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Listener per validare i campi in tempo reale
        toField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        subjectField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        bodyTextArea.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

        validateForm(); // Validazione iniziale
    }

    /**
     * Imposta il controller principale.
     * @param controller il controller principale
     */
    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    /**
     * Configura la vista per rispondere o inoltrare una email.
     * @param originalEmail l'email originale
     * @param isReply true se risposta, false se inoltro
     * @param isReplyAll true se rispondi a tutti
     */
    public void setupReplyOrForward(Email originalEmail, boolean isReply, boolean isReplyAll) {
        this.originalEmail = originalEmail;
        this.isReply = isReply;
        this.isReplyAll = isReplyAll;

        if (isReply) {
            setupReply(isReplyAll);
        } else {
            setupForward();
        }
    }

    /**
     * Prepara la vista per la risposta (o risposta a tutti).
     * @param replyAll true se rispondi a tutti
     */
    private void setupReply(boolean replyAll) {
        // Imposta i destinatari
        if (replyAll) {
            // Rispondi a mittente + altri destinatari escluso l'utente corrente
            String currentUser = clientController.getModel().getUserEmail();
            List<String> allRecipients = originalEmail.getRecipients().stream()
                    .filter(email -> !email.equals(currentUser))
                    .collect(Collectors.toList());

            if (!originalEmail.getSender().equals(currentUser)) {
                allRecipients.add(0, originalEmail.getSender());
            }

            toField.setText(String.join(", ", allRecipients));
        } else {
            // Rispondi solo al mittente
            toField.setText(originalEmail.getSender());
        }

        // Imposta oggetto con prefisso "Re:"
        String subject = originalEmail.getSubject();
        if (!subject.toLowerCase().startsWith("re:")) {
            subject = "Re: " + subject;
        }
        subjectField.setText(subject);

        // Imposta corpo con messaggio originale quotato
        String quotedBody = "\n\n--- Messaggio originale ---\n" +
                "Da: " + originalEmail.getSender() + "\n" +
                "Data: " + originalEmail.getFormattedTimestamp() + "\n" +
                "A: " + String.join(", ", originalEmail.getRecipients()) + "\n" +
                "Oggetto: " + originalEmail.getSubject() + "\n\n" +
                originalEmail.getBody();

        bodyTextArea.setText(quotedBody);
        bodyTextArea.positionCaret(0); // Posiziona il cursore all'inizio
    }

    /**
     * Prepara la vista per l'inoltro.
     */
    private void setupForward() {
        // Campo destinatari vuoto
        toField.setText("");

        // Imposta oggetto con prefisso "Fwd:"
        String subject = originalEmail.getSubject();
        if (!subject.toLowerCase().startsWith("fwd:") && !subject.toLowerCase().startsWith("fw:")) {
            subject = "Fwd: " + subject;
        }
        subjectField.setText(subject);

        // Imposta corpo con messaggio inoltrato
        String forwardedBody = "\n\n--- Messaggio inoltrato ---\n" +
                "Da: " + originalEmail.getSender() + "\n" +
                "Data: " + originalEmail.getFormattedTimestamp() + "\n" +
                "A: " + String.join(", ", originalEmail.getRecipients()) + "\n" +
                "Oggetto: " + originalEmail.getSubject() + "\n\n" +
                originalEmail.getBody();

        bodyTextArea.setText(forwardedBody);
        bodyTextArea.positionCaret(0); // Posiziona il cursore all'inizio

        // Focus sul campo destinatari
        toField.requestFocus();
    }

    /**
     * Valida i campi del form e abilita/disabilita il pulsante invio.
     */
    private void validateForm() {
        boolean valid = !toField.getText().trim().isEmpty() &&
                !subjectField.getText().trim().isEmpty() &&
                !bodyTextArea.getText().trim().isEmpty();

        sendButton.setDisable(!valid);
    }

    /**
     * Gestisce l'evento di invio dell'email.
     * Recupera i dati dai campi della vista, valida i destinatari,
     * crea l'oggetto Email e avvia l'invio in un thread separato.
     * Mostra un messaggio di errore se il formato di un destinatario non è valido
     * o se l'invio fallisce, altrimenti chiude la finestra al termine dell'invio.
     */
    @FXML
    private void handleSend() {
        String toText = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyTextArea.getText().trim();

        // Parsing destinatari
        List<String> recipients = Arrays.stream(toText.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());

        // Validazione formato email per ogni destinatario
        for (String recipient : recipients) {
            if (!EmailValidator.isValidEmailFormat(recipient)) {
                showAlert("Errore", "Indirizzo email non valido: " + recipient, Alert.AlertType.ERROR);
                return;
            }
        }

        // Crea e invia l'email
        Email email = new Email(clientController.getModel().getUserEmail(), recipients, subject, body);

        sendButton.setDisable(true);
        sendButton.setText("Invio...");

        Thread sendThread = new Thread(() -> {
            String result = clientController.sendEmail(email);

            Platform.runLater(() -> {
                if (result == null) {
                    closeWindow();
                } else {
                    sendButton.setDisable(false);
                    sendButton.setText("Invia");
                    showAlert("Errore", result, Alert.AlertType.ERROR);
                }
            });
        });
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /**
     * Gestisce l'evento di annullamento.
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Chiude la finestra corrente.
     */
    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Mostra un messaggio di alert.
     * @param title titolo della finestra
     * @param message messaggio da mostrare
     * @param type tipo di alert
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(cancelButton.getScene().getWindow());
        alert.showAndWait();
    }
}