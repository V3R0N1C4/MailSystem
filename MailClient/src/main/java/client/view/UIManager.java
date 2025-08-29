package client.view;

import client.model.Email;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

/**
 * Gestisce lo stato e la visibilità dei componenti dell'interfaccia utente.
 */
public class UIManager {

    private final HBox authBox;
    private final HBox toolbarBox;
    private final SplitPane mainSplitPane;
    private final Label connectionStatus;
    private final Label senderLabel, recipientsLabel, subjectLabel, dateLabel;
    private final TextArea bodyTextArea;
    private final Button replyButton, replyAllButton, forwardButton, deleteButton;
    private final Stage primaryStage;

    public UIManager(Stage primaryStage, HBox authBox, HBox toolbarBox, SplitPane mainSplitPane, Label connectionStatus,
                     Label senderLabel, Label recipientsLabel, Label subjectLabel, Label dateLabel, TextArea bodyTextArea,
                     Button replyButton, Button replyAllButton, Button forwardButton, Button deleteButton) {
        this.primaryStage = primaryStage;
        this.authBox = authBox;
        this.toolbarBox = toolbarBox;
        this.mainSplitPane = mainSplitPane;
        this.connectionStatus = connectionStatus;
        this.senderLabel = senderLabel;
        this.recipientsLabel = recipientsLabel;
        this.subjectLabel = subjectLabel;
        this.dateLabel = dateLabel;
        this.bodyTextArea = bodyTextArea;
        this.replyButton = replyButton;
        this.replyAllButton = replyAllButton;
        this.forwardButton = forwardButton;
        this.deleteButton = deleteButton;
    }

    public void showLoginScreen() {
        authBox.setVisible(true);
        authBox.setManaged(true);
        toolbarBox.setVisible(false);
        toolbarBox.setManaged(false);
        mainSplitPane.setVisible(false);
        mainSplitPane.setManaged(false);
        primaryStage.setTitle("Mail Client");
        updateConnectionStatus(false); // Inizialmente non connesso
    }

    public void showMainInterface(String userEmail) {
        authBox.setVisible(false);
        authBox.setManaged(false);
        toolbarBox.setVisible(true);
        toolbarBox.setManaged(true);
        mainSplitPane.setVisible(true);
        mainSplitPane.setManaged(true);
        primaryStage.setTitle("Mail Client - " + userEmail);
    }

    public void updateConnectionStatus(boolean connected) {
        connectionStatus.setText(connected ? "Connesso" : "Non connesso");
        connectionStatus.setStyle(connected ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    public void displayEmailDetails(Email email) {
        senderLabel.setText(email.getSender());
        recipientsLabel.setText(String.join(", ", email.getRecipients()));
        subjectLabel.setText(email.getSubject());
        dateLabel.setText(email.getFormattedTimestamp());
        bodyTextArea.setText(email.getBody());
    }

    public void clearEmailDetails() {
        senderLabel.setText("");
        recipientsLabel.setText("");
        subjectLabel.setText("");
        dateLabel.setText("");
        bodyTextArea.setText("");
    }

    public void enableEmailActions(boolean enable, boolean isSentEmail) {
        replyButton.setDisable(!enable || isSentEmail);
        replyAllButton.setDisable(!enable || isSentEmail);
        forwardButton.setDisable(!enable);
        deleteButton.setDisable(!enable);
    }

    public void setLoadingState(Button button, boolean isLoading, String loadingText, String defaultText) {
        button.setDisable(isLoading);
        button.setText(isLoading ? loadingText : defaultText);
    }
}
