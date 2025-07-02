**Client di posta elettronica desktop** sviluppato in Java utilizzando JavaFX per l'interfaccia grafica.

## Architettura Generale

Il progetto segue il pattern **MVC (Model-View-Controller)** ed è strutturato come segue:

### 1. **Model (`ClientModel`)**
- **Gestione dati**: Mantiene due liste osservabili per le email ricevute (`inbox`) e inviate (`sentEmails`)
- **Autenticazione**: Valida l'indirizzo email dell'utente tramite il server
- **Sincronizzazione automatica**: Ogni 5 secondi controlla nuove email dal server
- **Monitoraggio connessione**: Ogni 10 secondi verifica lo stato della connessione
- **Operazioni email**: Invio, eliminazione e gestione delle email

### 2. **View (File FXML + Controller)**
- **`ClientView.fxml`**: Interfaccia principale con:
    - Schermata di login (campo email + bottone accedi)
    - Toolbar con bottoni per nuova email, aggiorna, stato connessione, logout
    - TabPane con due schede: "Posta in arrivo" e "Inviati"
    - Pannello dettagli email con bottoni per rispondere, inoltrare, eliminare

- **`ComposeView.fxml`**: Finestra per comporre nuove email con campi destinatari, oggetto e corpo del messaggio

### 3. **Controller**
- **`ClientController`**: Coordina le operazioni tra model e view
- **`ClientViewController`**: Gestisce l'interfaccia principale e gli eventi utente
- **`ComposeViewController`**: Gestisce la finestra di composizione email

### 4. **Network (`ServerConnection`)**
- **Comunicazione TCP**: Si connette al server su `localhost:8080`
- **Protocollo custom**: Invia comandi testuali al server:
    - `VALIDATE_EMAIL:email` - Valida un indirizzo
    - `SEND_EMAIL:json` - Invia una email (serializzata in JSON)
    - `GET_EMAILS:email,index` - Recupera nuove email
    - `DELETE_EMAIL:email,id,isSent` - Elimina una email
- **Serializzazione JSON**: Utilizza Gson per convertire oggetti Email

## Funzionalità Principali

### **Login e Autenticazione**
1. L'utente inserisce il proprio indirizzo email
2. Il client valida il formato email
3. Invia richiesta di validazione al server
4. Se valido, carica inbox e posta inviata

### **Gestione Email**
- **Ricezione**: Sincronizzazione automatica ogni 5 secondi per nuove email
- **Invio**: Composizione con validazione destinatari e invio tramite server
- **Risposta/Risposta a tutti**: Precompila destinatari e oggetto con prefisso "Re:"
- **Inoltra**: Precompila oggetto con prefisso "Fwd:" e include messaggio originale
- **Eliminazione**: Rimozione email dal server e aggiornamento interfaccia

### **Interfaccia Utente**
- **Liste personalizzate**: Visualizzazione email con mittente/destinatari, oggetto e data
- **Dettagli email**: Pannello con informazioni complete e bottoni azione
- **Stati interfaccia**: Login → Interfaccia principale → Logout
- **Indicatori stato**: Connessione server, progress bar durante operazioni

### **Gestione Connessione**
- **Monitoraggio automatico**: Controllo periodico dello stato connessione
- **Riconnessione**: Tentativo automatico di sincronizzazione quando torna online
- **Gestione errori**: Messaggi utente per problemi di rete

## Dipendenze e Configurazione

Il progetto utilizza:
- **JavaFX 17.0.15**: Per l'interfaccia grafica
- **Gson 2.10.1**: Per serializzazione JSON
- **Maven**: Per gestione dipendenze e build
- **Java 11**: Versione target

## Ciclo di Vita Applicazione

1. **Avvio**: `ClientApplication.main()` → JavaFX Application
2. **Login**: Validazione email e caricamento dati utente
3. **Sincronizzazione**: Background threads per aggiornamenti automatici
4. **Interazione**: Gestione eventi utente (componi, rispondi, elimina)
5. **Chiusura**: Shutdown graceful di thread e connessioni

Questo client fornisce un'interfaccia completa per la gestione della posta elettronica con funzionalità come sincronizzazione automatica, gestione offline/online e interfaccia intuitiva.