// FileManager.java - Gestione persistenza
package server.storage;

import server.model.Email;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileManager {
    private static final String DATA_DIR = "maildata";
    private final ConcurrentHashMap<String, Lock> fileLocks;

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

    public void saveEmails(String emailAddress, List<Email> emails) {
        Lock lock = fileLocks.computeIfAbsent(emailAddress, k -> new ReentrantLock());
        lock.lock();
        try {
            String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
                oos.writeObject(new ArrayList<>(emails));
            } catch (IOException e) {
                System.err.println("Errore nel salvare le email per " + emailAddress + ": " + e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Email> loadEmails(String emailAddress) {
        Lock lock = fileLocks.computeIfAbsent(emailAddress, k -> new ReentrantLock());
        lock.lock();
        try {
            String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";
            File file = new File(fileName);

            if (!file.exists()) {
                return new ArrayList<>();
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
                return (List<Email>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Errore nel caricare le email per " + emailAddress + ": " + e.getMessage());
                return new ArrayList<>();
            }
        } finally {
            lock.unlock();
        }
    }
}