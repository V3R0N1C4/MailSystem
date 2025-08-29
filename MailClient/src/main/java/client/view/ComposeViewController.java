package client.view;

import client.controller.ClientController;
import client.model.Email;
import client.model.EmailValidator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller per la vista di composizione email.
 */
public class ComposeViewController implements Initializable {
    @FXML private TextField toField;
    @FXML private TextField subjectField;
    @FXML private TextArea bodyTextArea;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;

    private ClientController clientController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        subjectField.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        bodyTextArea.textProperty().addListener((obs, oldVal, newVal) -> validateForm());
        validateForm();
    }

    public void setClientController(ClientController controller) {
        this.clientController = controller;
    }

    public void setupReplyOrForward(Email originalEmail, boolean isReply, boolean isReplyAll) {
        if (isReply) {
            setupReply(originalEmail, isReplyAll);
        } else {
            setupForward(originalEmail);
        }
    }

    private void setupReply(Email originalEmail, boolean replyAll) {
        String currentUser = clientController.getModel().getUserEmail();
        List<String> recipients;
        if (replyAll) {
            recipients = originalEmail.getRecipients().stream()
                    .filter(email -> !email.equals(currentUser))
                    .collect(Collectors.toList());
            if (!originalEmail.getSender().equals(currentUser)) {
                recipients.add(0, originalEmail.getSender());
            }
        } else {
            recipients = List.of(originalEmail.getSender());
        }
        toField.setText(String.join(", ", recipients));

        String subject = originalEmail.getSubject();
        subjectField.setText(subject.toLowerCase().startsWith("re:") ? subject : "Re: " + subject);

        bodyTextArea.setText(createQuotedBody(originalEmail));
        bodyTextArea.positionCaret(0);
    }

    private void setupForward(Email originalEmail) {
        toField.setText("");
        String subject = originalEmail.getSubject();
        subjectField.setText(subject.toLowerCase().startsWith("fwd:") || subject.toLowerCase().startsWith("fw:") ? subject : "Fwd: " + subject);
        bodyTextArea.setText(createQuotedBody(originalEmail).replace("Messaggio originale", "Messaggio inoltrato"));
        bodyTextArea.positionCaret(0);
        toField.requestFocus();
    }

    private String createQuotedBody(Email email) {
        return "\n\n--- Messaggio originale ---\n" +
                "Da: " + email.getSender() + "\n" +
                "Data: " + email.getFormattedTimestamp() + "\n" +
                "A: " + String.join(", ", email.getRecipients()) + "\n" +
                "Oggetto: " + email.getSubject() + "\n\n" +
                email.getBody();
    }

    private void validateForm() {
        boolean isValid = !toField.getText().trim().isEmpty() &&
                !subjectField.getText().trim().isEmpty() &&
                !bodyTextArea.getText().trim().isEmpty();
        sendButton.setDisable(!isValid);
    }

    @FXML
    private void handleSend() {
        List<String> recipients = Arrays.stream(toField.getText().trim().split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toList());

        for (String recipient : recipients) {
            if (!EmailValidator.isValidEmailFormat(recipient)) {
                DialogManager.showAlert("Errore", "Indirizzo email non valido: " + recipient, Alert.AlertType.ERROR);
                return;
            }
        }

        Email email = new Email(clientController.getModel().getUserEmail(), recipients, subjectField.getText().trim(), bodyTextArea.getText().trim());

        sendButton.setDisable(true);
        sendButton.setText("Invio...");

        clientController.sendEmailAsync(email, result -> {
            if (result == null) {
                closeWindow();
            } else {
                sendButton.setDisable(false);
                sendButton.setText("Invia");
                DialogManager.showAlert("Errore", result, Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
