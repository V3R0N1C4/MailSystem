package server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import server.model.*;


import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestisce la comunicazione con un singolo client.
 * Ogni istanza viene eseguita in un thread separato.
 */
public class ClientHandler implements Runnable {
    private final Socket clientSocket;      // Socket associato al client
    private final ServerModel model;        // Modello del server per accedere ai dati e alle operazioni
    private final Gson gson;                // Oggetto Gson per la serializzazione/deserializzazione JSON

    /**
     * Costruttore della classe ClientHandler.
     * @param clientSocket socket del client connesso
     * @param model modello del server
     */
    public ClientHandler(Socket clientSocket, ServerModel model) {
        this.clientSocket = clientSocket;
        this.model = model;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
                .create();
    }

    /**
     * Metodo principale eseguito dal thread.
     * Gestisce una singola richiesta per connessione.
     */
    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Legge SOLO UNA richiesta per connessione (come HTTP)
            String request = in.readLine();
            if (request != null) {
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

    /**
     * Gestisce la richiesta ricevuta dal client, smistandola in base al comando.
     * @param request richiesta ricevuta
     * @param out stream di output verso il client
     */
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
                case "GET_SENT_EMAILS":
                    handleGetSentEmails(data, out);
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

    /**
     * Gestisce la validazione di un indirizzo email.
     * @param email indirizzo email da validare
     * @param out stream di output verso il client
     */
    private void handleValidateEmail(String email, PrintWriter out) {
        boolean valid = model.isValidEmail(email);
        out.println(valid ? "OK:Email valida" : "ERROR:Email non esistente");
        model.addToLog("Validazione email " + email + ": " + (valid ? "valida" : "non valida"));
    }

    /**
     * Gestisce la richiesta di invio di una email da parte del client.
     * Deserializza l'oggetto Email dal formato JSON, verifica la validità del mittente e dei destinatari,
     * consegna l'email tramite il modello e invia la risposta al client.
     * In caso di errore, restituisce un messaggio di errore appropriato.
     *
     * @param emailJson email in formato JSON da inviare
     * @param out stream di output verso il client
     */
    private void handleSendEmail(String emailJson, PrintWriter out) {
        try {
            Email email = gson.fromJson(emailJson, Email.class);

            // Verifica mittente
            if (!model.isValidEmail(email.getSender())) {
                out.println("ERROR: Mittente non registrato: " + email.getSender());
                return;
            }

            // Verifica destinatari
            List<String> invalidRecipients = email.getRecipients().stream()
                    .filter(recipient -> !model.isValidEmail(recipient))
                    .collect(Collectors.toList());

            if (!invalidRecipients.isEmpty()) {
                out.println("ERROR: Destinatari non validi: " + String.join(", ", invalidRecipients));
                return;
            }

            model.deliverEmail(email);
            out.println("OK:Email inviata con successo");
            model.addToLog("Email inviata da: " + email.getSender());

        } catch (Exception e) {
            out.println("ERROR:Errore nell'invio dell'email: " + e.getMessage());
        }
    }

    /**
     * Gestisce la richiesta di recupero delle nuove email per un utente.
     * @param data dati della richiesta (email, indice di partenza)
     * @param out stream di output verso il client
     */
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

    /**
     * Gestisce la richiesta di recupero delle email inviate da un utente.
     * @param emailAddress indirizzo email dell'utente
     * @param out stream di output verso il client
     */
    private void handleGetSentEmails(String emailAddress, PrintWriter out) {
        try {
            if (!model.isValidEmail(emailAddress)) {
                out.println("ERROR:Email non valida");
                return;
            }

            List<Email> sentEmails = model.getSentEmails(emailAddress);
            String emailsJson = gson.toJson(sentEmails);
            out.println("OK:" + emailsJson);

        } catch (Exception e) {
            out.println("ERROR:Errore nel recuperare le email inviate: " + e.getMessage());
        }
    }

    /**
     * Gestisce la richiesta di eliminazione di una email.
     * @param data dati della richiesta (email, id email, flag inviata/ricevuta)
     * @param out stream di output verso il client
     */
    private void handleDeleteEmail(String data, PrintWriter out) {
        try {
            String[] parts = data.split(",");
            String emailAddress = parts[0];
            String emailId = parts[1];
            boolean isSent = Boolean.parseBoolean(parts[2]);

            boolean deleted = model.deleteEmail(emailAddress, emailId, isSent);
            out.println(deleted ? "OK:Email eliminata" : "ERROR:Email non trovata");
//            model.addToLog("Email eliminata da: " + emailAddress);

        } catch (Exception e) {
            out.println("ERROR:Errore nell'eliminazione dell'email: " + e.getMessage());
        }
    }
}