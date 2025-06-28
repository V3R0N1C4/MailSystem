// Email.java - Classe condivisa tra client e server
package server.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class Email implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String sender;
    private List<String> recipients;
    private String subject;
    private String body;
    private LocalDateTime timestamp;

    public Email(String sender, List<String> recipients, String subject, String body) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.recipients = recipients;
        this.subject = subject;
        this.body = body;
        this.timestamp = LocalDateTime.now();
    }

    // Getters e setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return String.format("[%s] %s - %s", getFormattedTimestamp(), sender, subject);
    }
}