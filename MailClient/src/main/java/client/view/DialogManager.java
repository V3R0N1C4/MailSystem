package client.view;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Classe di utilità per mostrare dialoghi e alert in modo centralizzato.
 * <p>
 * Fornisce metodi statici per uniformare la creazione di finestre modali (Alert/Confirmation)
 * all'interno dell'applicazione JavaFX.
 * </p>
 */
public class DialogManager {

    /**
     * Mostra un alert di un tipo specifico.
     * @param title   il titolo della finestra di alert
     * @param message il messaggio da visualizzare
     * @param type    il tipo di alert (es. ERROR, INFORMATION)
     */
    public static void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Mostra un dialogo di conferma.
     * @param title   il titolo della finestra
     * @param header  il testo dell'intestazione
     * @param content il testo del contenuto
     * @return un Optional contenente il ButtonType scelto dall'utente (es. OK o CANCEL)
     */
    public static Optional<ButtonType> showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}
