// Mailbox.java - Casella postale
package server.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;
import common.model.Email;

public class Mailbox {
    private String emailAddress;
    private ObservableList<Email> emails;
    private ObservableList<Email> sentEmails = FXCollections.observableArrayList();
    private int lastSyncIndex;

    public Mailbox(String emailAddress) {
        this.emailAddress = emailAddress;
        this.emails = FXCollections.observableArrayList();
        this.lastSyncIndex = 0;
    }

    public synchronized void addEmail(Email email) {
        emails.add(email);
    }

    public synchronized void addSentEmail(Email email) {
        sentEmails.add(email);
    }

    public synchronized List<Email> getNewEmails(int fromIndex) {
        if (fromIndex >= emails.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(emails.subList(fromIndex, emails.size()));
    }

    public synchronized boolean removeEmail(String emailId) {
        return emails.removeIf(email -> email.getId().equals(emailId));
    }

    // Getters e setters
    public String getEmailAddress() { return emailAddress; }
    public ObservableList<Email> getEmails() { return emails; }
    public ObservableList<Email> getSentEmails() { return sentEmails; }
    public int getEmailCount() { return emails.size(); }

    public void setEmails(List<Email> emailList) {
        emails.clear();
        emails.addAll(emailList);
    }

    public void setSentEmails(List<Email> sentEmailList) {
        sentEmails.clear();
        sentEmails.addAll(sentEmailList);
    }
}