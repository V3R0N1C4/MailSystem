package server.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;
import common.model.Email;

/**
 * Rappresenta una casella di posta elettronica con gestione delle email ricevute e inviate.
 */
public class Mailbox {
    private String emailAddress;                // Indirizzo email associato alla casella
    private ObservableList<Email> emails;       // Lista osservabile delle email ricevute
    private ObservableList<Email> sentEmails = FXCollections.observableArrayList();    // Lista osservabile delle email inviate
    private int lastSyncIndex;                  // Indice dell'ultima sincronizzazione (non utilizzato attivamente nel codice)

    /**
     * Costruttore della casella di posta.
     * @param emailAddress indirizzo email associato
     */
    public Mailbox(String emailAddress) {
        this.emailAddress = emailAddress;
        this.emails = FXCollections.observableArrayList();
        this.lastSyncIndex = 0;
    }

    /**
     * Aggiunge una nuova email ricevuta alla casella.
     * @param email email da aggiungere
     */
    public synchronized void addEmail(Email email) {
        emails.add(email);
    }

    /**
     * Aggiunge una nuova email inviata alla lista delle inviate.
     * @param email email inviata da aggiungere
     */
    public synchronized void addSentEmail(Email email) {
        sentEmails.add(email);
    }

    /**
     * Restituisce la lista delle nuove email ricevute a partire da un certo indice.
     * @param fromIndex indice di partenza
     * @return lista di nuove email
     */
    public synchronized List<Email> getNewEmails(int fromIndex) {
        if (fromIndex >= emails.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(emails.subList(fromIndex, emails.size()));
    }

    /**
     * Rimuove un'email dalla casella in base all'ID.
     * @param emailId ID dell'email da rimuovere
     * @return true se l'email è stata rimossa, false altrimenti
     */
    public synchronized boolean removeEmail(String emailId) {
        return emails.removeIf(email -> email.getId().equals(emailId));
    }

    // Getter e setter

    /**
     * Restituisce l'indirizzo email associato.
     * @return indirizzo email
     */
    public String getEmailAddress() { return emailAddress; }

    /**
     * Restituisce la lista osservabile delle email ricevute.
     * @return lista email ricevute
     */
    public ObservableList<Email> getEmails() { return emails; }

    /**
     * Restituisce la lista osservabile delle email inviate.
     * @return lista email inviate
     */
    public ObservableList<Email> getSentEmails() { return sentEmails; }

    /**
     * Restituisce il numero totale di email ricevute.
     * @return numero email ricevute
     */
    public int getEmailCount() { return emails.size(); }

    /**
     * Sostituisce la lista delle email ricevute con una nuova lista.
     * @param emailList nuova lista di email ricevute
     */
    public void setEmails(List<Email> emailList) {
        emails.clear();
        emails.addAll(emailList);
    }

    /**
     * Sostituisce la lista delle email inviate con una nuova lista.
     * @param sentEmailList nuova lista di email inviate
     */
    public void setSentEmails(List<Email> sentEmailList) {
        sentEmails.clear();
        sentEmails.addAll(sentEmailList);
    }
}