// ClientHandler.java - Gestione client
package server.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.model.Email;
import server.model.ServerModel;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final ServerModel model;
    private final Gson gson;

    public ClientHandler(Socket clientSocket, ServerModel model) {
        this.clientSocket = clientSocket;
        this.model = model;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String request;
            while ((request = in.readLine()) != null) {
                handleRequest(request, out);
            }

        } catch (IOException e) {
            model.addToLog("Errore nella comunicazione con il client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                model.addToLog("Connessione chiusa con: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                model.addToLog("Errore nella chiusura della connessione: " + e.getMessage());
            }
        }
    }

    private void handleRequest(String request, PrintWriter out) {
        try {
            String[] parts = request.split(":", 2);
            String command = parts[0];
            String data = parts.length > 1 ? parts[1] : "";

            switch (command) {
                case "VALIDATE_EMAIL":
                    handleValidateEmail(data, out);
                    break;
                case "SEND_EMAIL":
                    handleSendEmail(data, out);
                    break;
                case "GET_EMAILS":
                    handleGetEmails(data, out);
                    break;
                case "DELETE_EMAIL":
                    handleDeleteEmail(data, out);
                    break;
                default:
                    out.println("ERROR:Comando non riconosciuto");
            }
        } catch (Exception e) {
            out.println("ERROR:" + e.getMessage());
            model.addToLog("Errore nel processare richiesta: " + e.getMessage());
        }
    }

    private void handleValidateEmail(String email, PrintWriter out) {
        boolean valid = model.isValidEmail(email);
        out.println(valid ? "OK:Email valida" : "ERROR:Email non esistente");
        model.addToLog("Validazione email " + email + ": " + (valid ? "valida" : "non valida"));
    }

    private void handleSendEmail(String emailJson, PrintWriter out) {
        try {
            Email email = gson.fromJson(emailJson, Email.class);

            // Verifica destinatari
            for (String recipient : email.getRecipients()) {
                if (!model.isValidEmail(recipient)) {
                    out.println("ERROR:Destinatario non valido: " + recipient);
                    return;
                }
            }

            model.deliverEmail(email);
            out.println("OK:Email inviata con successo");

        } catch (Exception e) {
            out.println("ERROR:Errore nell'invio dell'email: " + e.getMessage());
        }
    }

    private void handleGetEmails(String data, PrintWriter out) {
        try {
            String[] parts = data.split(",");
            String emailAddress = parts[0];
            int fromIndex = Integer.parseInt(parts[1]);

            if (!model.isValidEmail(emailAddress)) {
                out.println("ERROR:Email non valida");
                return;
            }

            List<Email> newEmails = model.getNewEmails(emailAddress, fromIndex);
            String emailsJson = gson.toJson(newEmails);
            out.println("OK:" + emailsJson);

        } catch (Exception e) {
            out.println("ERROR:Errore nel recuperare le email: " + e.getMessage());
        }
    }

    private void handleDeleteEmail(String data, PrintWriter out) {
        try {
            String[] parts = data.split(",");
            String emailAddress = parts[0];
            String emailId = parts[1];

            boolean deleted = model.deleteEmail(emailAddress, emailId);
            out.println(deleted ? "OK:Email eliminata" : "ERROR:Email non trovata");

        } catch (Exception e) {
            out.println("ERROR:Errore nell'eliminazione dell'email: " + e.getMessage());
        }
    }
}