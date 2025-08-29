package client.controller;

import client.model.ClientModel;
import client.model.Email;
import javafx.application.Platform;

import java.util.function.Consumer;

/**
 * Controller principale per la gestione delle operazioni del client email.
 * Gestisce la logica di business e l'esecuzione di task in background.
 */
public class ClientController {
    private final ClientModel model;

    public ClientController() {
        this.model = new ClientModel();
    }

    /**
     * Esegue l'autenticazione dell'utente in un thread separato.
     * @param email l'email dell'utente
     * @param callback il callback da eseguire sul thread FX con il risultato (true/false)
     */
    public void authenticateUserAsync(String email, Consumer<Boolean> callback) {
        Thread authThread = new Thread(() -> {
            boolean success = model.authenticateUser(email);
            Platform.runLater(() -> callback.accept(success));
        });
        authThread.setDaemon(true);
        authThread.start();
    }

    /**
     * Invia un'email in un thread separato.
     * @param email l'email da inviare
     * @param callback il callback da eseguire sul thread FX con il risultato (null in caso di successo, altrimenti messaggio di errore)
     */
    public void sendEmailAsync(Email email, Consumer<String> callback) {
        Thread sendThread = new Thread(() -> {
            String result = model.sendEmail(email);
            if (result == null) {
                model.addToSentEmails(email);
            }
            Platform.runLater(() -> callback.accept(result));
        });
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /**
     * Elimina un'email in un thread separato.
     * @param email l'email da eliminare
     * @param isSent true se l'email è nella cartella inviati
     * @param callback il callback da eseguire sul thread FX con il risultato (true/false)
     */
    public void deleteEmailAsync(Email email, boolean isSent, Consumer<Boolean> callback) {
        Thread deleteThread = new Thread(() -> {
            boolean success = model.deleteEmail(email, isSent);
            Platform.runLater(() -> callback.accept(success));
        });
        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    /**
     * Esegue lo shutdown del modello.
     */
    public void shutdown() {
        model.shutdown();
    }

    /**
     * Restituisce il modello associato.
     * @return il ClientModel
     */
    public ClientModel getModel() {
        return model;
    }
}
