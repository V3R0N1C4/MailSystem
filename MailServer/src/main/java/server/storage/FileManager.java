package server.storage;

import common.model.Email;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Gestisce la persistenza delle mailbox degli utenti su file.
 */
public class FileManager {
    // Directory dove vengono salvati i dati delle mailbox
    private static final String DATA_DIR = "maildata";
    // Mappa per gestire i lock sui file delle mailbox, uno per ogni utente
    private final ConcurrentHashMap<String, Lock> fileLocks;

    /**
     * Classe interna che rappresenta i dati di una mailbox,
     * contenente sia le email ricevute che quelle inviate.
     */
    public static class MailboxData implements Serializable {
        private final List<Email> receivedEmails;
        private final List<Email> sentEmails;

        /**
         * Costruttore della MailboxData.
         * @param receivedEmails lista delle email ricevute
         * @param sentEmails lista delle email inviate
         */
        public MailboxData(List<Email> receivedEmails, List<Email> sentEmails) {
            this.receivedEmails = receivedEmails;
            this.sentEmails = sentEmails;
        }

        /**
         * Restituisce la lista delle email ricevute.
         * @return lista di Email ricevute
         */
        public List<Email> getReceivedEmails() {
            return receivedEmails;
        }

        /**
         * Restituisce la lista delle email inviate.
         * @return lista di Email inviate
         */
        public List<Email> getSentEmails() {
            return sentEmails;
        }
    }

    /**
     * Costruttore della classe FileManager.
     * Inizializza la mappa dei lock e crea la directory dati se non esiste.
     */
    public FileManager() {
        this.fileLocks = new ConcurrentHashMap<>();
        createDataDirectory();
    }

    /**
     * Crea la directory dei dati se non esiste già.
     */
    private void createDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Salva la mailbox di un utente su file in modo thread-safe.
     * @param emailAddress indirizzo email dell'utente
     * @param receivedEmails lista delle email ricevute
     * @param sentEmails lista delle email inviate
     */
    public void saveMailbox(String emailAddress, List<Email> receivedEmails, List<Email> sentEmails) {
        // Ottiene o crea un lock per l'utente
        Lock lock = fileLocks.computeIfAbsent(emailAddress, k -> new ReentrantLock());
        lock.lock();
        try {
            // Costruisce il nome del file a partire dall'indirizzo email
            String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";

            // Scrive l'oggetto MailboxData su file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
                MailboxData data = new MailboxData(new ArrayList<>(receivedEmails), new ArrayList<>(sentEmails));
                oos.writeObject(data);
            } catch (IOException e) {
                System.err.println("Errore nel salvare la mailbox per " + emailAddress + ": " + e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Carica la mailbox di un utente da file in modo thread-safe.
     * Se il file non esiste, restituisce una mailbox vuota.
     * Gestisce la compatibilità con il vecchio formato (solo lista di email ricevute).
     * @param emailAddress indirizzo email dell'utente
     * @return oggetto MailboxData con le email ricevute e inviate
     */
    @SuppressWarnings("unchecked")
    public MailboxData loadMailbox(String emailAddress) {
        // Ottiene o crea un lock per l'utente
        Lock lock = fileLocks.computeIfAbsent(emailAddress, k -> new ReentrantLock());
        lock.lock();
        try {
            // Costruisce il nome del file a partire dall'indirizzo email
            String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";
            File file = new File(fileName);

            // Se il file non esiste, restituisce una mailbox vuota
            if (!file.exists()) {
                return new MailboxData(new ArrayList<>(), new ArrayList<>());
            }

            // Legge l'oggetto dal file
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                Object obj = ois.readObject();

                // Gestione compatibilità con vecchio formato (solo lista di email ricevute)
                if (obj instanceof ArrayList) {
                    List<Email> oldList = (ArrayList<Email>) obj;
                    return new MailboxData(oldList, new ArrayList<>());
                } else if (obj instanceof MailboxData) {
                    return (MailboxData) obj;
                } else {
                    throw new IOException("Formato file non supportato");
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Errore nel caricare la mailbox per " + emailAddress + ": " + e.getMessage());
                return new MailboxData(new ArrayList<>(), new ArrayList<>());
            }
        } finally {
            lock.unlock();
        }
    }
}