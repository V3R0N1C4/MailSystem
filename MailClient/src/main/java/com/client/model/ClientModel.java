package client.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import client.network.ServerConnection;
import client.model.Email;
import client.model.EmailValidator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Modello principale del client per la gestione delle email.
 */
public class ClientModel {
    private String userEmail;                               // Email dell'utente autenticato
    private ObservableList<client.model.Email> inbox;       // Lista delle email ricevute
    private ObservableList<client.model.Email> sentEmails;  // Lista delle email inviate
    private ServerConnection serverConnection;              // Gestione della connessione al server
    private ScheduledExecutorService scheduler;             // Scheduler per attività periodiche
    private int lastEmailIndex;                             // Indice dell'ultima email ricevuta
    private boolean connected;                              // Stato della connessione

    /**
     * Costruttore: inizializza le liste, la connessione e lo scheduler.
     */
    public ClientModel() {
        this.inbox = FXCollections.observableArrayList();
        this.sentEmails = FXCollections.observableArrayList();
        this.serverConnection = new ServerConnection();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.lastEmailIndex = 0;
        this.connected = false;
    }

    /**
     * Autentica l'utente tramite email.
     * @param email email da autenticare
     * @return true se autenticato, false altrimenti
     */
    public boolean authenticateUser(String email) {
        if (EmailValidator.isValidEmailFormat(email)) {
            boolean valid = serverConnection.validateEmail(email);
            if (valid) {
                this.userEmail = email;
                this.connected = true;

                // Carica sia inbox che messaggi inviati
                List<Email> received = serverConnection.getNewEmails(email, 0);
                List<Email> sent = serverConnection.getSentEmails(email);

                inbox.setAll(received);
                sentEmails.setAll(sent);
                lastEmailIndex = inbox.size();

                startAutoSync();
                return true;
            }
        }
        return false;
    }

    /**
     * Avvia la sincronizzazione automatica e il controllo connessione.
     */
    private void startAutoSync() {
        // Sincronizzazione automatica ogni 5 secondi
        scheduler.scheduleAtFixedRate(this::syncWithServer, 0, 5, TimeUnit.SECONDS);

        // Verifica connessione ogni 10 secondi
        scheduler.scheduleAtFixedRate(this::checkConnection, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Sincronizza la casella di posta con il server.
     */
    private void syncWithServer() {
        if (userEmail != null && connected) {
            try {
                List<client.model.Email> newEmails = serverConnection.getNewEmails(userEmail, lastEmailIndex);
                if (newEmails != null && !newEmails.isEmpty()) {
                    Platform.runLater(() -> {
                        inbox.addAll(newEmails);
                        lastEmailIndex = inbox.size();
                        // Notifica nuovo messaggio (può essere implementata con Alert)
                    });
                }
            } catch (Exception e) {
                connected = false;
                System.err.println("Errore nella sincronizzazione: " + e.getMessage());
            }
        }
    }

    /**
     * Controlla periodicamente lo stato della connessione.
     */
    private void checkConnection() {
        if (userEmail != null) {
            boolean wasConnected = connected;
            connected = serverConnection.testConnection();

            if (!wasConnected && connected) {
                Platform.runLater(() -> {
                    // Riconnesso - sincronizza
                    syncWithServer();
                });
            }
        }
    }

    /**
     * Invia una email tramite il server.
     * @param email email da inviare
     * @return true se inviata, false altrimenti
     */
    public boolean sendEmail(client.model.Email email) {
        if (connected) {
            return serverConnection.sendEmail(email);
        }
        return false;
    }

    /**
     * Elimina una email dalla casella di posta.
     * @param email email da eliminare
     * @return true se eliminata, false altrimenti
     */
    public boolean deleteEmail(client.model.Email email) {
        if (connected && userEmail != null) {
            boolean deleted = serverConnection.deleteEmail(userEmail, email.getId());
            if (deleted) {
                Platform.runLater(() -> inbox.remove(email));
                lastEmailIndex = inbox.size();
            }
            return deleted;
        }
        return false;
    }

    /**
     * Arresta lo scheduler e chiude la connessione al server.
     */
    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        if (serverConnection != null) {
            serverConnection.close();
        }
    }

    /**
     * Aggiunge una email alla lista degli inviati.
     * @param email email inviata
     */
    public void addToSentEmails(client.model.Email email) {
        Platform.runLater(() -> sentEmails.add(email));
    }

    // Getter
    public String getUserEmail() { return userEmail; }
    public ObservableList<client.model.Email> getInbox() { return inbox; }
    public ObservableList<client.model.Email> getSentEmails() { return sentEmails; }
    public boolean isConnected() { return connected; }
}