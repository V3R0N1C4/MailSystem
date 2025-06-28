// ServerConnection.java - Connessione al server
package client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import client.model.Email;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class ServerConnection {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private final Gson gson;

    public ServerConnection() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new client.model.LocalDateTimeTypeAdapter())
                .create();
    }

    public boolean validateEmail(String email) {
        return sendRequest("VALIDATE_EMAIL:" + email).startsWith("OK");
    }

    public boolean sendEmail(Email email) {
        String emailJson = gson.toJson(email);
        return sendRequest("SEND_EMAIL:" + emailJson).startsWith("OK");
    }

    public List<Email> getNewEmails(String emailAddress, int fromIndex) {
        String response = sendRequest("GET_EMAILS:" + emailAddress + "," + fromIndex);
        if (response.startsWith("OK:")) {
            String emailsJson = response.substring(3);
            TypeToken<List<Email>> typeToken = new TypeToken<List<Email>>() {};
            return gson.fromJson(emailsJson, typeToken.getType());
        }
        return null;
    }

    public List<Email> getSentEmails(String emailAddress) {
        String response = sendRequest("GET_SENT_EMAILS:" + emailAddress);
        if (response.startsWith("OK:")) {
            String emailsJson = response.substring(3);
            return gson.fromJson(emailsJson, new TypeToken<List<Email>>(){}.getType());
        }
        return Collections.emptyList();
    }

    public boolean deleteEmail(String emailAddress, String emailId) {
        return sendRequest("DELETE_EMAIL:" + emailAddress + "," + emailId).startsWith("OK");
    }

    public boolean testConnection() {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

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

    public void close() {
        // Cleanup se necessario
    }
}