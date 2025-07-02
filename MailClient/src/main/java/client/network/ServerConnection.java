package client.network;

// Importazione delle librerie necessarie
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import common.model.LocalDateTimeTypeAdapter;
import common.model.Email;

/**
 * Classe che gestisce la connessione al server per l'invio e la ricezione di email.
 */
public class ServerConnection {
    private static final String SERVER_HOST = "localhost";  // Costante per l'host del server
    private static final int SERVER_PORT = 8080;            // Costante per la porta del server
    private final Gson gson;                                // Oggetto Gson per la serializzazione/deserializzazione JSON

    /**
     * Costruttore: inizializza Gson con l'adapter per LocalDateTime.
     */
    public ServerConnection() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
    }

    /**
     * Valida un indirizzo email tramite richiesta al server.
     * @param email indirizzo email da validare
     * @return true se l'email è valida, false altrimenti
     */
    public boolean validateEmail(String email) {
        return sendRequest("VALIDATE_EMAIL:" + email).startsWith("OK");
    }

    /**
     * Invia una email al server serializzando l'oggetto Email in formato JSON.
     * @param email oggetto Email da inviare
     * @return risposta del server come stringa (ad esempio "OK" o messaggio di errore)
     */
    public String sendEmail(Email email) {
        String emailJson = gson.toJson(email);
        return sendRequest("SEND_EMAIL:" + emailJson);
    }

    /**
     * Recupera le nuove email per un determinato indirizzo a partire da un indice.
     * @param emailAddress indirizzo email dell'utente
     * @param fromIndex indice da cui recuperare le email
     * @return lista di email ricevute o null in caso di errore
     */
    public List<Email> getNewEmails(String emailAddress, int fromIndex) {
        String response = sendRequest("GET_EMAILS:" + emailAddress + "," + fromIndex);
        if (response.startsWith("OK:")) {
            String emailsJson = response.substring(3);
            TypeToken<List<Email>> typeToken = new TypeToken<List<Email>>() {};
            return gson.fromJson(emailsJson, typeToken.getType());
        }
        return null;
    }

    /**
     * Recupera le email inviate da un determinato indirizzo.
     * @param emailAddress indirizzo email dell'utente
     * @return lista di email inviate o lista vuota in caso di errore
     */
    public List<Email> getSentEmails(String emailAddress) {
        String response = sendRequest("GET_SENT_EMAILS:" + emailAddress);
        if (response.startsWith("OK:")) {
            String emailsJson = response.substring(3);
            return gson.fromJson(emailsJson, new TypeToken<List<Email>>(){}.getType());
        }
        return Collections.emptyList();
    }

    /**
     * Elimina una email (ricevuta o inviata) dal server.
     * @param emailAddress indirizzo email dell'utente
     * @param emailId identificativo dell'email da eliminare
     * @param isSent true se si tratta di una email inviata, false se ricevuta
     * @return true se l'eliminazione ha avuto successo, false altrimenti
     */
    public boolean deleteEmail(String emailAddress, String emailId, boolean isSent) {
        return sendRequest("DELETE_EMAIL:" + emailAddress + "," + emailId + "," + isSent).startsWith("OK");
    }

    /**
     * Verifica la connessione al server.
     * @return true se la connessione è riuscita, false altrimenti
     */
    public boolean testConnection() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Invia una richiesta al server e restituisce la risposta.
     * @param request stringa della richiesta da inviare
     * @return risposta del server come stringa
     */
    private String sendRequest(String request) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(request);
            return in.readLine();

        } catch (Exception e) {
            return "ERROR:Errore di connessione al server";
        }
    }

    /**
     * Metodo per eventuali operazioni di cleanup (attualmente vuoto).
     */
    public void close() {
        // Cleanup se necessario
    }
}