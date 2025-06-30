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

/**
 * Modello principale del server che gestisce le caselle di posta, il log del server
 * e l'interazione con il FileManager per la persistenza dei dati.
 */
public class ServerModel {
    private Map<String, Mailbox> mailboxes;     // Mappa che associa ogni indirizzo email alla relativa Mailbox
    private ObservableList<String> serverLog;   // Lista osservabile per il log del server (usata per aggiornare la GUI)
    private FileManager fileManager;            // Gestore per il salvataggio e caricamento delle mailbox su disco

    /**
     * Costruttore: inizializza le strutture dati, crea account predefiniti e carica le mailbox.
     */
    public ServerModel() {
        this.mailboxes = new HashMap<>();
        this.serverLog = FXCollections.observableArrayList();
        this.fileManager = new FileManager();

        // Inizializza account predefiniti
        initializeDefaultAccounts();
        // Carica le mailbox da file
        loadMailboxes();
    }

    /**
     * Crea alcuni account email predefiniti e li aggiunge alla mappa delle mailbox.
     */
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

    /**
     * Verifica se un indirizzo email esiste tra le mailbox gestite dal server.
     * @param email indirizzo email da verificare
     * @return true se l'email esiste, false altrimenti
     */
    public synchronized boolean isValidEmail(String email) {
        return mailboxes.containsKey(email);
    }

    /**
     * Consegnare un'email: la salva sia nella posta inviata del mittente che nella posta in arrivo dei destinatari.
     * @param email oggetto Email da consegnare
     */
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

    /**
     * Restituisce la lista delle nuove email ricevute da un certo indice in poi.
     * @param emailAddress indirizzo email della mailbox
     * @param fromIndex indice da cui partire
     * @return lista di Email o null se la mailbox non esiste
     */
    public synchronized List<Email> getNewEmails(String emailAddress, int fromIndex) {
        server.model.Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            return mailbox.getNewEmails(fromIndex);
        }
        return null;
    }

    /**
     * Restituisce la lista delle email inviate da un certo indirizzo.
     * @param emailAddress indirizzo email della mailbox
     * @return lista di Email inviate o null se la mailbox non esiste
     */
    public synchronized List<Email> getSentEmails(String emailAddress) {
        Mailbox mailbox = mailboxes.get(emailAddress);
        if (mailbox != null) {
            return new ArrayList<>(mailbox.getSentEmails());
        }
        return null;
    }

    /**
     * Elimina una email dalla mailbox specificata (posta inviata o ricevuta).
     * @param emailAddress indirizzo email della mailbox
     * @param emailId id dell'email da eliminare
     * @param isSent true se si tratta di una email inviata, false se ricevuta
     * @return true se l'email è stata eliminata, false altrimenti
     */
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

    /**
     * Aggiunge un messaggio al log del server, con timestamp, in modo thread-safe sulla GUI.
     * @param message messaggio da aggiungere al log
     */
    public void addToLog(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            serverLog.add("[" + timestamp + "] " + message);
        });
    }

    /**
     * Carica le email ricevute e inviate per ogni mailbox dagli archivi su disco.
     */
    private void loadMailboxes() {
        for (String email : mailboxes.keySet()) {
            FileManager.MailboxData data = fileManager.loadMailbox(email);
            mailboxes.get(email).setEmails(data.getReceivedEmails());
            mailboxes.get(email).setSentEmails(data.getSentEmails());
        }
    }

    /**
     * Salva la mailbox specificata su disco.
     * @param email indirizzo email della mailbox da salvare
     */
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

    // Getter per il log del server
    public ObservableList<String> getServerLog() { return serverLog; }
    // Getter per la mappa delle mailbox
    public Map<String, Mailbox> getMailboxes() { return mailboxes; }
}