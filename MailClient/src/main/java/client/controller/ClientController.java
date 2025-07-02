package client.controller;

import client.model.ClientModel;
import common.model.Email;

/**
 * Controller principale per la gestione delle operazioni del client email.
 */
public class ClientController {
    // Modello che gestisce i dati e le operazioni principali
    private ClientModel model;

    /**
     * Costruttore che inizializza il modello.
     */
    public ClientController() {
        this.model = new ClientModel();
    }

    /**
     * Autentica un utente dato l'indirizzo email.
     * @param email l'indirizzo email dell'utente
     * @return true se l'autenticazione ha successo, false altrimenti
     */
    public boolean authenticateUser(String email) {
        return model.authenticateUser(email);
    }

    /**
     * Invia una email tramite il modello.
     * Se l'invio ha successo (ovvero il risultato è null), aggiunge l'email alla lista delle email inviate.
     * @param email l'oggetto Email da inviare
     * @return null se l'invio ha successo, altrimenti una stringa con il messaggio di errore
     */
    public String sendEmail(Email email) {
        String result = model.sendEmail(email);
        if (result == null) {
            model.addToSentEmails(email);
        }
        return result;
    }

    /**
     * Elimina una email specificando se è un'email inviata.
     * @param email l'oggetto Email da eliminare
     * @param isSent true se è un'email inviata, false se è in ricevuta
     * @return true se l'eliminazione ha successo, false altrimenti
     */
    public boolean deleteEmail(Email email, boolean isSent) {
        return model.deleteEmail(email, isSent);
    }

    /**
     * Esegue le operazioni di chiusura del modello.
     */
    public void shutdown() {
        model.shutdown();
    }

    /**
     * Restituisce il modello associato al controller.
     * @return il ClientModel
     */
    public ClientModel getModel() {
        return model;
    }
}