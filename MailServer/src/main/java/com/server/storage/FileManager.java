// FileManager.java - Gestione persistenza
package server.storage;

import server.model.Email;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private static final String DATA_DIR = "maildata";

    public FileManager() {
        createDataDirectory();
    }

    private void createDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public synchronized void saveEmails(String emailAddress, List<Email> emails) {
        String fileName = DATA_DIR + File.separator + emailAddress.replace("@", "_") + ".dat";

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(new ArrayList<>(emails));
        } catch (IOException e) {
            System.err.println("Errore nel salvare le email per " + emailAddress + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized List<Email> loadEmails(String emailAddress) {
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
    }
}