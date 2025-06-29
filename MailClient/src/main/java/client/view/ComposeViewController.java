package client.view;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import client.controller.ClientController;
import client.model.EmailValidator;
import common.model.Email;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ComposeViewController implements Initializable {
    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyTextArea;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private ClientController clientController;
    private Email originalEmail;
    private boolean isReply;
    private boolean isReplyAll;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set up validation
        toField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        subjectField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        bodyTextArea.textProperty().addListener((obs, oldVal, newVal) -> validateForm());

        validateForm(); // Initial validation
    }

    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

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

    private void setupReply(boolean replyAll) {
        // Set recipients
        if (replyAll) {
            // Reply to sender + all other recipients except current user
            String currentUser = clientController.getModel().getUserEmail();
            List<String> allRecipients = originalEmail.getRecipients().stream()
                    .filter(email -> !email.equals(currentUser))
                    .collect(Collectors.toList());

            if (!originalEmail.getSender().equals(currentUser)) {
                allRecipients.add(0, originalEmail.getSender());
            }

            toField.setText(String.join(", ", allRecipients));
        } else {
            // Reply only to sender
            toField.setText(originalEmail.getSender());
        }

        // Set subject with "Re:" prefix
        String subject = originalEmail.getSubject();
        if (!subject.toLowerCase().startsWith("re:")) {
            subject = "Re: " + subject;
        }
        subjectField.setText(subject);

        // Set body with quoted original message
        String quotedBody = "\n\n--- Messaggio originale ---\n" +
                "Da: " + originalEmail.getSender() + "\n" +
                "Data: " + originalEmail.getFormattedTimestamp() + "\n" +
                "A: " + String.join(", ", originalEmail.getRecipients()) + "\n" +
                "Oggetto: " + originalEmail.getSubject() + "\n\n" +
                originalEmail.getBody();

        bodyTextArea.setText(quotedBody);
        bodyTextArea.positionCaret(0); // Move cursor to beginning
    }

    private void setupForward() {
        // Leave "To" field empty for user to fill
        toField.setText("");

        // Set subject with "Fwd:" prefix
        String subject = originalEmail.getSubject();
        if (!subject.toLowerCase().startsWith("fwd:") && !subject.toLowerCase().startsWith("fw:")) {
            subject = "Fwd: " + subject;
        }
        subjectField.setText(subject);

        // Set body with forwarded message
        String forwardedBody = "\n\n--- Messaggio inoltrato ---\n" +
                "Da: " + originalEmail.getSender() + "\n" +
                "Data: " + originalEmail.getFormattedTimestamp() + "\n" +
                "A: " + String.join(", ", originalEmail.getRecipients()) + "\n" +
                "Oggetto: " + originalEmail.getSubject() + "\n\n" +
                originalEmail.getBody();

        bodyTextArea.setText(forwardedBody);
        bodyTextArea.positionCaret(0); // Move cursor to beginning

        // Focus on "To" field
        toField.requestFocus();
    }

    private void validateForm() {
        boolean valid = !toField.getText().trim().isEmpty() &&
                !subjectField.getText().trim().isEmpty() &&
                !bodyTextArea.getText().trim().isEmpty();

        sendButton.setDisable(!valid);
    }

    @FXML
    private void handleSend() {
        String toText = toField.getText().trim();
        String subject = subjectField.getText().trim();
        String body = bodyTextArea.getText().trim();

        // Parse recipients
        List<String> recipients = Arrays.stream(toText.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());

        // Validate email formats
        for (String recipient : recipients) {
            if (!EmailValidator.isValidEmailFormat(recipient)) {
                showAlert("Errore", "Indirizzo email non valido: " + recipient, Alert.AlertType.ERROR);
                return;
            }
        }

        // Create and send email
        Email email = new Email(clientController.getModel().getUserEmail(), recipients, subject, body);

        sendButton.setDisable(true);
        sendButton.setText("Invio...");

        Thread sendThread = new Thread(() -> {
            boolean success = clientController.sendEmail(email);

            javafx.application.Platform.runLater(() -> {
                if (success) {
                    closeWindow();
                } else {
                    sendButton.setDisable(false);
                    sendButton.setText("Invia");
                    showAlert("Errore", "Impossibile inviare l'email. Verifica la connessione al server.",
                            Alert.AlertType.ERROR);
                }
            });
        });
        sendThread.setDaemon(true);
        sendThread.start();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(cancelButton.getScene().getWindow());
        alert.showAndWait();
    }
}