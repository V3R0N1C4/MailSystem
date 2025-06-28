package client.controller;

import client.model.ClientModel;
import client.model.Email;

public class ClientController {
    private ClientModel model;

    public ClientController() {
        this.model = new ClientModel();
    }

    public boolean authenticateUser(String email) {
        return model.authenticateUser(email);
    }

    public boolean sendEmail(Email email) {
        return model.sendEmail(email);
    }

    public boolean deleteEmail(Email email) {
        return model.deleteEmail(email);
    }

    public void shutdown() {
        model.shutdown();
    }

    public ClientModel getModel() {
        return model;
    }
}