// ServerModel.java - Modello del server
package server.model;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;

import common.model.Email;
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
                "cl16@mail.com",
                "mv33@mail.com",
                "op81@mail.com"
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
        // Salva nella casella di posta inviata del mittente
        if (isValidEmail(email.getSender())) {
            mailboxes.get(email.getSender()).addSentEmail(email);
            saveMailbox(email.getSender());
        }

        // Salva nella casella di posta in arrivo dei destinatari
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
        server.model.Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            return mailbox.getNewEmails(fromIndex);
        }
        return null;
    }

    public synchronized List<Email> getSentEmails(String emailAddress) {
        Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            return new ArrayList<>(mailbox.getSentEmails());
        }
        return null;
    }

    public synchronized boolean deleteEmail(String emailAddress, String emailId, boolean isSent) {
        Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            boolean deleted;
            if (isSent) {
                // Elimina da sentEmails
                deleted = mailbox.getSentEmails().removeIf(email -> email.getId().equals(emailId));
            } else {
                // Elimina da receivedEmails
                deleted = mailbox.removeEmail(emailId);
            }

            if (deleted) {
                saveMailbox(emailAddress);
                addToLog("Email eliminata per: " + emailAddress + " (tipo: " + (isSent ? "INVIATA" : "RICEVUTA") + ")");
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
            FileManager.MailboxData data = fileManager.loadMailbox(email);
            mailboxes.get(email).setEmails(data.getReceivedEmails());
            mailboxes.get(email).setSentEmails(data.getSentEmails());
        }
    }

    private void saveMailbox(String email) {
        Mailbox mailbox = mailboxes.get(email);
        if (mailbox != null) {
            fileManager.saveMailbox(
                    email,
                    mailbox.getEmails(),
                    mailbox.getSentEmails()
            );
        }
    }

    // Getters
    public ObservableList<String> getServerLog() { return serverLog; }
    public Map<String, Mailbox> getMailboxes() { return mailboxes; }
}