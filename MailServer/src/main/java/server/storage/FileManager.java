// FileManager.java - Gestione persistenza
package server.storage;

import common.model.Email;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileManager {
    private static final String DATA_DIR = "maildata";
    private final ConcurrentHashMap<String, Lock> fileLocks;

    // Classe interna per memorizzare entrambe le liste
    public static class MailboxData implements Serializable {
        private final List<Email> receivedEmails;
        private final List<Email> sentEmails;

        public MailboxData(List<Email> receivedEmails, List<Email> sentEmails) {
            this.receivedEmails = receivedEmails;
            this.sentEmails = sentEmails;
        }

        // Aggiungi getter
        public List<Email> getReceivedEmails() {
            return receivedEmails;
        }

        // Aggiungi getter
        public List<Email> getSentEmails() {
            return sentEmails;
        }
    }

    public FileManager() {
        this.fileLocks = new ConcurrentHashMap<>();
        createDataDirectory();
    }

    private void createDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void saveMailbox(String emailAddress, List<Email> receivedEmails, List<Email> sentEmails) {
        Lock lock = fileLocks.computeIfAbsent(emailAddress, k -> new ReentrantLock());
        lock.lock();
        try {
            String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";

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

    @SuppressWarnings("unchecked")
    public MailboxData loadMailbox(String emailAddress) {
        Lock lock = fileLocks.computeIfAbsent(emailAddress, k -> new ReentrantLock());
        lock.lock();
        try {
            String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";
            File file = new File(fileName);

            if (!file.exists()) {
                return new MailboxData(new ArrayList<>(), new ArrayList<>());
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                Object obj = ois.readObject();

                // Gestione compatibilità con vecchio formato
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