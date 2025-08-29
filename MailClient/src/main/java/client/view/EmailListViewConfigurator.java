package client.view;

import client.model.Email;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Classe di utilità per configurare le ListView di email.
 */
public class EmailListViewConfigurator {

    /**
     * Configura una ListView per visualizzare le email in modo personalizzato.
     * @param listView la ListView da configurare
     * @param isSentFolder true se la lista è per la cartella "inviati"
     * @param onEmailSelected un callback da eseguire quando un'email viene selezionata
     */
    public static void configure(ListView<Email> listView, boolean isSentFolder, Consumer<Email> onEmailSelected) {
        listView.setCellFactory(lv -> new EmailListCell(isSentFolder));

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                onEmailSelected.accept(newVal);
            }
        });
    }

    /**
     * Classe interna per la visualizzazione personalizzata delle email nelle ListView.
     */
    private static class EmailListCell extends ListCell<Email> {
        private final boolean isSentFolder;

        public EmailListCell(boolean isSentFolder) {
            this.isSentFolder = isSentFolder;
        }

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
                    subjectLabel.setText("A: " + String.join(", ", email.getRecipients()));
                    detailsLabel.setText("Oggetto: " + email.getSubject());
                } else {
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
