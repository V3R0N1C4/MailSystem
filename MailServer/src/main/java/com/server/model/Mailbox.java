// Mailbox.java - Casella postale
package server.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;

public class Mailbox {
    private String emailAddress;
    private ObservableList<server.model.Email> emails;
    private int lastSyncIndex;

    public Mailbox(String emailAddress) {
        this.emailAddress = emailAddress;
        this.emails = FXCollections.observableArrayList();
        this.lastSyncIndex = 0;
    }

    public synchronized void addEmail(server.model.Email email) {
        emails.add(email);
    }

    public synchronized List<server.model.Email> getNewEmails(int fromIndex) {
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
    public ObservableList<server.model.Email> getEmails() { return emails; }
    public int getEmailCount() { return emails.size(); }
    public void setEmails(List<server.model.Email> emailList) {
        emails.clear();
        emails.addAll(emailList);
    }
}