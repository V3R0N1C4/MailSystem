// ServerModel.java - Modello del server
package server.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import server.storage.FileManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerModel {
    private Map<String, Mailbox> mailboxes;
    private ObservableList<String> serverLog;
    private FileManager fileManager;

    public ServerModel() {
        this.mailboxes = new HashMap<>();
        this.serverLog = FXCollections.observableArrayList();
        this.fileManager = new FileManager();

        // Inizializza account predefiniti
        initializeDefaultAccounts();
        loadMailboxes();
    }

    private void initializeDefaultAccounts() {
        String[] defaultAccounts = {
                "alice@mailserver.com",
                "bob@mailserver.com",
                "charlie@mailserver.com"
        };

        for (String account : defaultAccounts) {
            mailboxes.put(account, new Mailbox(account));
        }

        addToLog("Server inizializzato con " + defaultAccounts.length + " account");
    }

    public synchronized boolean isValidEmail(String email) {
        return mailboxes.containsKey(email);
    }

    public synchronized void deliverEmail(Email email) {
        for (String recipient : email.getRecipients()) {
            if (isValidEmail(recipient)) {
                mailboxes.get(recipient).addEmail(email);
                saveMailbox(recipient);
                addToLog("Email consegnata a: " + recipient + " da: " + email.getSender());
            } else {
                addToLog("ERRORE: Destinatario non valido: " + recipient);
            }
        }
    }

    public synchronized List<Email> getNewEmails(String emailAddress, int fromIndex) {
        Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            return mailbox.getNewEmails(fromIndex);
        }
        return null;
    }

    public synchronized boolean deleteEmail(String emailAddress, String emailId) {
        Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            boolean deleted = mailbox.removeEmail(emailId);
            if (deleted) {
                saveMailbox(emailAddress);
                addToLog("Email eliminata per: " + emailAddress);
            }
            return deleted;
        }
        return false;
    }

    public void addToLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            serverLog.add("[" + timestamp + "] " + message);
        });
    }

    private void loadMailboxes() {
        for (String email : mailboxes.keySet()) {
            List<Email> emails = fileManager.loadEmails(email);
            mailboxes.get(email).setEmails(emails);
        }
    }

    private void saveMailbox(String email) {
        Mailbox mailbox = mailboxes.get(email);
        if (mailbox != null) {
            fileManager.saveEmails(email, mailbox.getEmails());
        }
    }

    // Getters
    public ObservableList<String> getServerLog() { return serverLog; }
    public Map<String, Mailbox> getMailboxes() { return mailboxes; }
}
