package server.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Rappresenta una casella di posta elettronica con gestione delle email ricevute e inviate.
 */
public class Mailbox {
    private String emailAddress;                // Indirizzo email associato alla casella
    private List<Email> emails;                 // Lista delle email ricevute
    private List<Email> sentEmails;             // Lista delle email inviate
    private int lastSyncIndex;                  // Indice dell'ultima sincronizzazione (non utilizzato attivamente nel codice)

    /**
     * Costruttore della casella di posta.
     * @param emailAddress indirizzo email associato
     */
    public Mailbox(String emailAddress) {
        this.emailAddress = emailAddress;
        this.emails = new ArrayList<>();
        this.sentEmails = new ArrayList<>();
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

    public String getEmailAddress() { return emailAddress; }

    public List<Email> getEmails() { return emails; }

    public List<Email> getSentEmails() { return sentEmails; }

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