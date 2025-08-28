package client.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import client.network.ServerConnection;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Modello principale del client per la gestione delle email.
 * Gestisce autenticazione, sincronizzazione, invio, eliminazione e stato della connessione.
 */
public class ClientModel {
    private String userEmail;                   // Email dell'utente autenticato
    private ObservableList<Email> inbox;        // Lista delle email ricevute (inbox)
    private ObservableList<Email> sentEmails;   // Lista delle email inviate
    private ServerConnection serverConnection;  // Gestione della connessione al server
    private ScheduledExecutorService scheduler; // Scheduler per attività periodiche (sync e controllo connessione)
    private int lastEmailIndex;                 // Indice dell'ultima email ricevuta (per sincronizzazione incrementale)
    private boolean connected;                  // Stato della connessione al server

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
     * Se l'autenticazione va a buon fine, carica inbox e sent, avvia la sincronizzazione automatica.
     * @param email email da autenticare
     * @return true se autenticato, false altrimenti
     */
    public boolean authenticateUser(String email) {
        if (EmailValidator.isValidEmailFormat(email)) {
            boolean valid = serverConnection.validateEmail(email);
            if (valid) {
                this.userEmail = email;
                this.connected = true;

                // Recupera email ricevute e inviate dal server
                List<Email> received = serverConnection.getNewEmails(email, 0);
                List<Email> sent = serverConnection.getSentEmails(email);

                // Aggiorna le ObservableList sul thread FX
                Platform.runLater(() -> {
                    inbox.setAll(received);
                    sentEmails.setAll(sent);
                });

                lastEmailIndex = received.size();
                startAutoSync();
                return true;
            }
        }
        return false;
    }

    /**
     * Avvia la sincronizzazione automatica e il controllo connessione.
     * Sincronizza la casella di posta ogni 5 secondi e controlla la connessione ogni 10 secondi.
     */
    private void startAutoSync() {
        // Sincronizzazione automatica ogni 5 secondi
        scheduler.scheduleAtFixedRate(this::syncWithServer, 0, 5, TimeUnit.SECONDS);

        // Verifica connessione ogni 10 secondi
        scheduler.scheduleAtFixedRate(this::checkConnection, 0, 10, TimeUnit.SECONDS);
    }

    /**
     * Sincronizza la casella di posta con il server.
     * Recupera nuove email e aggiorna la inbox.
     */
    private void syncWithServer() {
        if (userEmail != null && connected) {
            try {
                List<Email> newEmails = serverConnection.getNewEmails(userEmail, lastEmailIndex);
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
     * Se la connessione viene ristabilita, sincronizza la casella di posta.
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
     * Se la connessione è attiva, invia l'oggetto Email tramite il metodo sendEmail del ServerConnection.
     * Restituisce null se l'invio ha successo (risposta che inizia con "OK"), altrimenti restituisce il messaggio di errore senza il prefisso "ERROR:".
     * Se non connesso, restituisce un messaggio di errore specifico.
     *
     * @param email l'oggetto Email da inviare
     * @return null se inviata con successo, altrimenti una stringa con il messaggio di errore
     */
    public String sendEmail(Email email) {
        if (connected) {
            String response = serverConnection.sendEmail(email);
            if (response.startsWith("OK")) {
                return null; // Successo
            } else {
                return response.replaceFirst("ERROR:", ""); // Rimuove il prefisso ERROR
            }
        }
        return "Non connesso al server";
    }

    /**
     * Elimina una email dalla casella di posta.
     * @param email email da eliminare
     * @return true se eliminata, false altrimenti
     */
    public boolean deleteEmail(Email email, boolean isSent) {
        if (connected && userEmail != null) {
            boolean deleted = serverConnection.deleteEmail(userEmail, email.getId(), isSent);
            if (deleted) {
                Platform.runLater(() -> {
                    if (isSent) {
                        sentEmails.remove(email);
                    } else {
                        inbox.remove(email);
                        lastEmailIndex = inbox.size();
                    }
                });
            }
            return deleted;
        }
        return false;
    }

    /**
     * Arresta lo scheduler e chiude la connessione al server.
     * Da chiamare in fase di chiusura dell'applicazione.
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
    public void addToSentEmails(Email email) {
        Platform.runLater(() -> sentEmails.add(email));
    }

    // Getter per i campi principali
    /**
     * Restituisce l'email dell'utente autenticato.
     * @return email utente
     */
    public String getUserEmail() { return userEmail; }

    /**
     * Restituisce la lista delle email ricevute (inbox).
     * @return inbox
     */
    public ObservableList<Email> getInbox() { return inbox; }

    /**
     * Restituisce la lista delle email inviate.
     * @return sentEmails
     */
    public ObservableList<Email> getSentEmails() { return sentEmails; }

    /**
     * Restituisce lo stato della connessione.
     * @return true se connesso, false altrimenti
     */
    public boolean isConnected() { return connected; }
}