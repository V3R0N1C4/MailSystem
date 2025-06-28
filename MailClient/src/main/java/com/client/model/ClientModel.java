// ClientModel.java - Modello del client
package client.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import client.network.ServerConnection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientModel {
    private String userEmail;
    private ObservableList<Email> inbox;
    private ServerConnection serverConnection;
    private ScheduledExecutorService scheduler;
    private int lastEmailIndex;
    private boolean connected;

    public ClientModel() {
        this.inbox = FXCollections.observableArrayList();
        this.serverConnection = new ServerConnection();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.lastEmailIndex = 0;
        this.connected = false;
    }

    public boolean authenticateUser(String email) {
        if (EmailValidator.isValidEmailFormat(email)) {
            boolean valid = serverConnection.validateEmail(email);
            if (valid) {
                this.userEmail = email;
                this.connected = true;
                startAutoSync();
                return true;
            }
        }
        return false;
    }

    private void startAutoSync() {
        // Sincronizzazione automatica ogni 5 secondi
        scheduler.scheduleAtFixedRate(this::syncWithServer, 0, 5, TimeUnit.SECONDS);

        // Verifica connessione ogni 10 secondi
        scheduler.scheduleAtFixedRate(this::checkConnection, 0, 10, TimeUnit.SECONDS);
    }

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

    public boolean sendEmail(Email email) {
        if (connected) {
            return serverConnection.sendEmail(email);
        }
        return false;
    }

    public boolean deleteEmail(Email email) {
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

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        if (serverConnection != null) {
            serverConnection.close();
        }
    }

    // Getters
    public String getUserEmail() { return userEmail; }
    public ObservableList<client.model.Email> getInbox() { return inbox; }
    public boolean isConnected() { return connected; }
}