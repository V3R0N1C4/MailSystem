# Analisi Dettagliata del Progetto MailServer

Questo documento fornisce un'analisi completa del progetto `MailServer`, descrivendo l'architettura, i componenti chiave e il flusso di lavoro. L'obiettivo è offrire una guida chiara per comprendere e studiare il codice sorgente.

## Architettura Generale

Il `MailServer` è un'applicazione Java costruita con le seguenti tecnologie e principi:

- **Java 11**: Versione del linguaggio utilizzata per lo sviluppo.
- **JavaFX**: Utilizzato per creare una semplice interfaccia grafica (GUI) che mostra i log delle attività del server in tempo reale.
- **Socket Programming**: Il server comunica con i client tramite socket TCP/IP. Utilizza un'architettura multi-threaded per gestire più client contemporaneamente.
- **Maven**: Usato come strumento di build e gestione delle dipendenze.
- **Gson**: Libreria di Google per la serializzazione e deserializzazione di oggetti Java in formato JSON, utilizzata per lo scambio di dati complessi (come le email) con i client.
- **Persistenza su File**: Le caselle di posta degli utenti (email ricevute e inviate) vengono salvate sul disco locale tramite serializzazione di oggetti Java.

L'architettura segue il pattern **Model-View-Controller (MVC)** in modo approssimativo:
- **Model**: Le classi nel package `server.model` (es. `Email`, `Mailbox`, `ServerModel`) rappresentano i dati e la logica di business.
- **View**: Il file FXML (`ServerView.fxml`) definisce la struttura dell'interfaccia utente.
- **Controller**: Il `ServerViewController` gestisce l'interazione tra la vista e il modello, mentre il `ClientHandler` agisce come controller per le richieste di rete.

## Flusso di Lavoro Principale

1.  **Avvio**: L'applicazione viene avviata tramite la classe `ServerApplication`. Questa inizializza l'interfaccia JavaFX e il `ServerViewController`.
2.  **Inizializzazione del Server**: Il `ServerViewController` crea un'istanza del `ServerModel` e avvia il `SocketServer` in un thread separato.
3.  **Ascolto delle Connessioni**: Il `SocketServer` si mette in ascolto sulla porta 8080.
4.  **Gestione Client**: Quando un client si connette, il `SocketServer` accetta la connessione e crea un nuovo thread con un'istanza di `ClientHandler` per gestire specificamente quel client.
5.  **Elaborazione Richieste**: Il `ClientHandler` legge la richiesta del client (es. "SEND_EMAIL:{...}"), la interpreta e invoca i metodi appropriati sul `ServerModel`.
6.  **Interazione con i Dati**: Il `ServerModel` esegue la logica di business, come consegnare un'email o recuperare messaggi, interagendo con gli oggetti `Mailbox`.
7.  **Persistenza**: Le modifiche alle caselle di posta vengono salvate su file tramite il `FileManager` per garantire che i dati non vengano persi alla chiusura del server.
8.  **Risposta al Client**: Il `ClientHandler` invia una risposta al client ("OK" o "ERROR") e chiude la connessione.
9.  **Logging**: Tutte le operazioni significative vengono registrate nel `ServerModel` e visualizzate nella `ListView` dell'interfaccia grafica.
10. **Arresto**: Quando l'utente chiude la finestra, il `ServerViewController` chiama il metodo `shutdown()`, che arresta il `SocketServer` in modo pulito.

---

## Analisi dei File per Ordine di Studio

### 1. I Modelli di Dati (`src/main/java/server/model/`)

#### `Email.java`
- **Scopo**: Rappresenta una singola email. È un POJO (Plain Old Java Object) che implementa `Serializable` per poter essere salvato su file.
- **Campi Chiave**:
    - `id`: `String` - UUID univoco per ogni email.
    - `sender`: `String` - Indirizzo email del mittente.
    - `recipients`: `List<String>` - Lista di destinatari.
    - `subject`: `String` - Oggetto dell'email.
    - `body`: `String` - Corpo del messaggio.
    - `timestamp`: `LocalDateTime` - Data e ora di creazione.

#### `Mailbox.java`
- **Scopo**: Rappresenta la casella di posta di un utente.
- **Campi Chiave**:
    - `emails`: `ObservableList<Email>` - Lista delle email ricevute.
    - `sentEmails`: `ObservableList<Email>` - Lista delle email inviate.
- **Caratteristiche**: I metodi sono `synchronized` per garantire la thread-safety, dato che più `ClientHandler` potrebbero tentare di accedere alla stessa casella di posta.

#### `EmailValidator.java`
- **Scopo**: Classe di utilità con un metodo statico `isValidEmailFormat(String email)` che usa una regex per validare il formato di un indirizzo email.

#### `LocalDateTimeTypeAdapter.java`
- **Scopo**: Adattatore per la libreria Gson. Converte gli oggetti `LocalDateTime` in stringhe in formato ISO 8601 e viceversa durante la serializzazione/deserializzazione JSON. È necessario perché Gson non supporta nativamente i tipi di data/ora di Java 8+.

### 2. La Persistenza dei Dati (`src/main/java/server/storage/`)

#### `FileManager.java`
- **Scopo**: Gestisce il salvataggio e il caricamento delle caselle di posta su file nella directory `maildata/`.
- **Funzionamento**:
    - **Salvataggio (`saveMailbox`)**: Serializza un oggetto `MailboxData` (che contiene le liste di email ricevute e inviate) in un file binario `.dat`.
    - **Caricamento (`loadMailbox`)**: Deserializza il file `.dat` per recuperare lo stato di una casella di posta. Gestisce anche la compatibilità con un vecchio formato di file.
    - **Thread-Safety**: Utilizza una `ConcurrentHashMap` di `ReentrantLock` per associare un lock a ogni file di mailbox. Questo impedisce race condition quando più thread tentano di accedere allo stesso file contemporaneamente.

### 3. La Logica di Business (`src/main/java/server/model/ServerModel.java`)

- **Scopo**: È il cuore pulsante del server. Centralizza tutta la logica di business.
- **Funzioni Principali**:
    - Gestisce una mappa in memoria di tutte le caselle di posta (`Mailbox`).
    - `isValidEmail(String email)`: Controlla se un utente esiste.
    - `deliverEmail(Email email)`: Aggiunge l'email alla cartella "inviata" del mittente e alle caselle di posta di tutti i destinatari. Chiama `FileManager` per persistere le modifiche.
    - `getNewEmails(...)`, `getSentEmails(...)`, `deleteEmail(...)`: Fornisce le operazioni CRUD (Create, Read, Update, Delete) sulle email.
    - `getServerLog()`: Restituisce una `ObservableList<String>` a cui l'interfaccia grafica si collega per visualizzare i log.
    - `addToLog(String message)`: Aggiunge un nuovo messaggio ai log.

### 4. La Gestione della Rete (`src/main/java/server/network/` e `controller/`)

#### `SocketServer.java`
- **Scopo**: Ascolta le connessioni TCP in ingresso.
- **Funzionamento**:
    - Implementa `Runnable` per essere eseguito in un thread separato.
    - In un ciclo `while(running)`, attende le connessioni dei client con `serverSocket.accept()`.
    - Per ogni connessione, crea un nuovo `ClientHandler` e lo avvia in un nuovo thread, passando il socket del client e un riferimento al `ServerModel`.

#### `ClientHandler.java`
- **Scopo**: Gestisce la comunicazione con un singolo client per la durata di una singola richiesta.
- **Funzionamento**:
    - Legge la richiesta dal client, formattata come `COMANDO:DATI`.
    - Usa uno `switch` sul `COMANDO` per decidere quale azione eseguire.
    - Deserializza i `DATI` da JSON a oggetti Java (es. `Email`) usando Gson.
    - Chiama i metodi appropriati sul `ServerModel` per eseguire l'operazione richiesta.
    - Serializza la risposta in JSON se necessario.
    - Invia una risposta al client (`OK:` o `ERROR:`).
    - Chiude la connessione.

### 5. L'Interfaccia Utente (`src/main/java/server/view/` e `controller/`)

#### `ServerViewController.java`
- **Scopo**: Il controller per la GUI del server.
- **Funzionamento**:
    - `initialize()`: Metodo chiamato all'avvio. Inizializza il `ServerModel`, collega la `logListView` ai log del modello e avvia il `SocketServer`.
    - `shutdown()`: Metodo chiamato alla chiusura della finestra. Arresta il `SocketServer` per una chiusura pulita.

### 6. Il Punto di Ingresso (`src/main/java/server/ServerApplication.java`)

- **Scopo**: Classe principale che avvia l'applicazione JavaFX.
- **Funzionamento**:
    - Il metodo `main` chiama `launch(args)`.
    - Il metodo `start` carica l'interfaccia dal file FXML, imposta la scena e gestisce l'evento di chiusura della finestra per chiamare `controller.shutdown()`.

### 7. Configurazione del Progetto (`pom.xml`)

- **Scopo**: File di configurazione di Maven.
- **Sezioni Chiave**:
    - `<properties>`: Specifica che il progetto usa Java 11.
    - `<dependencies>`: Dichiara le dipendenze necessarie: `javafx-controls`, `javafx-fxml` e `gson`.
    - `<plugins>`: Configura il `javafx-maven-plugin` per poter eseguire l'applicazione direttamente da Maven, specificando `server.ServerApplication` come classe principale.
