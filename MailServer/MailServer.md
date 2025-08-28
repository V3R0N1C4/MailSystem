# MailServer — Guida Definitiva, passo per passo

Questa è la guida completa e ragionata del progetto MailServer. È pensata per farti capire ogni parte del codice, come si avvia in locale, come funziona sotto il cofano, e come estenderlo in modo sicuro. Al termine della lettura avrai piena padronanza dell’architettura, del protocollo di rete e dei dettagli d’implementazione.

Indice rapido:

- Panoramica e obiettivi
- Setup ambiente (Windows + VS Code)
- Avvio rapido (Maven/JavaFX)
- Architettura e threading
- Walkthrough del codice (classe per classe)
- Protocollo di rete (formati, esempi)
- Persistenza su disco (formati, locking)
- Estensioni e manutenzione
- Troubleshooting (errori comuni, soluzioni)

## Panoramica

Tecnologie chiave:

- Java (target: 11; compilabile con JDK più recenti)
- JavaFX (GUI per i log del server)
- Sockets TCP (multi-thread, un client per thread)
- Maven (build, dipendenze, esecuzione)
- Gson (serializzazione/deserializzazione JSON)
- Persistenza su file (serializzazione Java, compatibilità retro)

Pattern architetturale: Model–View–Controller (MVC) pragmatico.

- Model: `server.model.*`
- View: `src/main/resources/server/view/ServerView.fxml`
- Controller GUI: `server.view.ServerViewController`
- Controller rete: `server.controller.ClientHandler`

Flusso principale (end-to-end):

1) `ServerApplication` avvia la GUI JavaFX e carica `ServerView.fxml`.
2) `ServerViewController.initialize` crea `ServerModel`, collega la ListView ai log, avvia `SocketServer` su porta 8080.
3) `SocketServer` accetta connessioni, ciascuna gestita da un `ClientHandler` in un thread separato.
4) `ClientHandler` legge una singola riga di richiesta `COMANDO:DATI`, invoca `ServerModel` e risponde `OK:...` o `ERROR:...`.
5) `ServerModel` aggiorna caselle (`Mailbox`), salva su disco tramite `FileManager` e scrive sul log (aggiornato in GUI).
6) Alla chiusura finestra, `ServerApplication` invoca `controller.shutdown()`, che ferma `SocketServer`.

## Setup ambiente (Windows + VS Code)

Prerequisiti:

- JDK installato (JDK 11 o superiore). In questo progetto è configurato target 11; funziona anche con JDK 17/21/24.
- Maven installato e raggiungibile come `mvn` (o specifica il path completo a `mvn.cmd`).
- VS Code con estensioni Java (Language Support for Java) e Maven (facoltativa ma utile).

Impostare Maven in VS Code (se richiesto):

- Impostazione “Maven › Executable: Path”: inserisci il percorso completo del binario Maven su Windows:
  `C:\Users\yasse\Tools\apache-maven-3.9.9\bin\mvn.cmd`
- In alternativa, aggiungi a livello utente nelle impostazioni JSON:
  "maven.executable.path": "C:\\Users\\yasse\\Tools\\apache-maven-3.9.9\\bin\\mvn.cmd"
- Dopo l’impostazione, riavvia VS Code o “Developer: Reload Window”.

Nota JDK vs JavaFX: non tentare di eseguire la classe `server.ServerApplication` con un semplice `java ...` senza moduli JavaFX sul classpath/module-path. Usa Maven (plugin JavaFX) per l’esecuzione: si occuperà lui delle dipendenze JavaFX.

## Avvio rapido (build ed esecuzione)

Apri un nuovo terminale in VS Code nella cartella del progetto e:

- Verifica Maven: `mvn -v`
- Build: `mvn -DskipTests package`
- Esegui GUI JavaFX (server): `mvn -Djavafx.platform=win javafx:run`

Esito atteso:

- Build: `BUILD SUCCESS`
- Avvio: finestra “Mail Server” e log iniziale (es. “Server avviato sulla porta 8080”).

Perché non usare `java -cp ... server.ServerApplication`? Perché JavaFX non è parte del JDK dal Java 11 in poi: il plugin Maven aggiunge i moduli corretti. Eseguendo “a mano” va impostato manualmente il module-path (sconsigliato in questo contesto).

## Architettura e threading

Componenti principali:

- GUI JavaFX: mostra i log del server in tempo reale.
- `SocketServer` (thread dedicato): accetta connessioni e crea un `ClientHandler` per ciascun client.
- `ClientHandler` (uno per connessione): legge 1 richiesta, la elabora e chiude.
- `ServerModel`: logica di business; gestisce caselle (`Mailbox`) e interagisce con `FileManager`.
- `FileManager`: persiste su disco (thread-safe via lock per mailbox).

Thread-safety:

- `SocketServer` gira finché `running==true`; la `accept()` è bloccante.
- `ClientHandler`: per ogni richiesta, singolo giro di vita; usa `Gson` per JSON e scrive log.
- `Mailbox` usa metodi `synchronized` per mutazioni sicure.
- `FileManager` usa un `ReentrantLock` per utente (chiave: indirizzo email) in una `ConcurrentHashMap`.
- Logs: `ServerModel.addToLog` usa `Platform.runLater` per aggiornare in sicurezza la `ObservableList` legata alla GUI.

## Walkthrough del codice (classe per classe)

Percorso sorgente: `src/main/java/server/**`

### server.ServerApplication

Punto d’ingresso JavaFX.

- Carica FXML: `/server/view/ServerView.fxml`.
- Costruisce la `Scene` (600x400), imposta titolo.
- Recupera `ServerViewController` e registra handler di chiusura finestra che invoca `controller.shutdown()`.
- `main(String[] args)` → `launch(args)`.

Concetto chiave: tutta la parte di rete non è avviata qui, ma nel controller della vista (separazione ruoli GUI/business/rete).

### server.view.ServerViewController

Controller della GUI.

- `initialize(...)`:
  - Crea `ServerModel`.
  - `logListView.setItems(model.getServerLog())` per log in tempo reale.
  - Crea `SocketServer(8080, model)`, lo avvia in un thread `daemon`.
  - Log iniziale: “Server avviato sulla porta 8080”.
- `shutdown()`:
  - Ferma `SocketServer.stop()` e logga “Server arrestato”.

Nota: La porta 8080 è hardcoded. Per cambiarla, modifica il costruttore in `initialize`.

### server.network.SocketServer

Accetta connessioni client.

- Campi: `port`, `model`, `ServerSocket serverSocket`, `volatile boolean running`.
- `run()`:
  - `serverSocket = new ServerSocket(port)`; log: “Server in ascolto sulla porta …”.
  - Loop finché `running`: `accept()` → crea `ClientHandler` → `new Thread(handler).start()`.
  - Errori di `accept`: logga solo se `running` (così lo stop non spammerà errori).
- `stop()`:
  - `running=false` e `serverSocket.close()` se aperto.

### server.controller.ClientHandler

Gestisce UNA richiesta per connessione (stile HTTP 1.0 “short-lived”).

- Costruito con `Socket clientSocket` e `ServerModel model`.
- Usa `Gson` con `LocalDateTimeTypeAdapter` per serializzare date ISO.
- `run()`:
  - `BufferedReader`/`PrintWriter` sul socket.
  - `String request = in.readLine()`; se non null → `handleRequest(request, out)`.
  - Chiude connessione e logga chiusura.
- `handleRequest(String request, PrintWriter out)`
  - Parsing: `COMANDO:DATI` (split con `":"`, max 2 parti).
  - Switch:
    - `VALIDATE_EMAIL`
    - `SEND_EMAIL`
    - `GET_EMAILS`
    - `GET_SENT_EMAILS`
    - `DELETE_EMAIL`
    - default → `ERROR:Comando non riconosciuto`

Handler specifici:

- `handleValidateEmail(email, out)` → `model.isValidEmail(email)` → `OK:Email valida` o `ERROR:Email non esistente`.
- `handleSendEmail(emailJson, out)` → `Email` da JSON → verifica mittente e destinatari → `model.deliverEmail(email)` → `OK:Email inviata con successo` o `ERROR:...` con dettaglio.
- `handleGetEmails("email,fromIndex", out)` → ritorna `OK:[...]` con lista JSON delle nuove ricevute a partire da `fromIndex`.
- `handleGetSentEmails(email, out)` → `OK:[...]` con lista JSON inviate.
- `handleDeleteEmail("email,emailId,isSent", out)` → elimina e salva → `OK:Email eliminata` o `ERROR:Email non trovata`.

### server.model.ServerModel

Cuore della logica.

- Strutture:
  - `Map<String, Mailbox> mailboxes` (in memoria)
  - `ObservableList<String> serverLog` (GUI)
  - `FileManager fileManager`
- Costruttore:
  - `initializeDefaultAccounts()` → crea caselle predefinite: `cl16@mail.com`, `mv33@mail.com`, `op81@mail.com`. Logga il conteggio.
  - `loadMailboxes()` → carica ricevute e inviate da disco per ciascuna casella.
- Metodi principali:
  - `isValidEmail(String email)` → esistenza in `mailboxes`.
  - `deliverEmail(Email email)` → valida mittente e destinatari; aggiorna inviate/ricevute; salva ogni mailbox toccata; logga consegne ed errori.
  - `getNewEmails(String email, int fromIndex)` → lista nuove ricevute da indice (o `null` se mailbox assente).
  - `getSentEmails(String email)` → copia lista inviate (o `null`).
  - `deleteEmail(String email, String emailId, boolean isSent)` → rimuove, salva e logga; ritorna boolean.
  - `addToLog(String message)` → aggiorna lista log con timestamp `HH:mm:ss` usando `Platform.runLater`.
  - `getServerLog()` e `getMailboxes()` getter.

Nota: alcuni metodi restituiscono `null` se la mailbox non esiste. I client dovrebbero gestire `ERROR` se si passa un indirizzo non valido.

### server.model.Mailbox

Rappresenta la casella utente.

- Campi: `emailAddress`, `ObservableList<Email> emails` (ricevute), `ObservableList<Email> sentEmails` (inviate), `lastSyncIndex` (non usato).
- Metodi sincronizzati per aggiungere/rimuovere e per ottenere “nuove” email da un indice.
- `setEmails`/`setSentEmails` rimpiazzano i contenuti mantenendo liste osservabili (utile per GUI).

### server.model.Email

Modello email serializzabile.

- Campi: `id` (UUID), `sender`, `recipients`, `subject`, `body`, `timestamp`.
- Crea id/timestamp nel costruttore.
- `getFormattedTimestamp()` e `toString()` per rappresentazioni leggibili.

### server.model.EmailValidator

Utilità per convalida formato email (regex). Usalo lato client/validazione preliminare; lato server la validità “logica” è decisa da `ServerModel.isValidEmail`.

### server.model.LocalDateTimeTypeAdapter

Gson `TypeAdapter<LocalDateTime>`: serializza/deserializza in ISO_LOCAL_DATE_TIME. Garantisce interoperabilità JSON coerente.

### server.storage.FileManager

Persistenza su disco per ciascun utente.

- Directory dati: `maildata/`.
- File per utente: `maildata/<email_con_@_sostituita_da_->_underscore>.dat`, es.: `cl16_mail.com.dat` per `cl16@mail.com`.
- Classe interna `MailboxData` contiene due liste: ricevute e inviate.
- Lock per-utente con `ConcurrentHashMap<String, Lock>` per evitare corruzione quando più thread salvano/caricano.
- Compatibilità retro: se il file contiene una semplice `ArrayList<Email>` (vecchio formato), viene mappata su “ricevute” e “inviate” vuote.

### FXML: `src/main/resources/server/view/ServerView.fxml`

Layout semplice in `VBox`:

- Etichetta titolo: “Mail Server - Log Eventi”.
- `ListView` (id: `logListView`) per i log in tempo reale.
- `HBox` di stato con `Label` (`statusLabel`, “Server in esecuzione”).
Il controller associato è `server.view.ServerViewController`.

## Protocollo di rete

Il protocollo è a testo semplice, una riga per richiesta e una per risposta. Formato generale:

- Richiesta: `COMANDO:DATI` (il separatore `:` divide il comando dai dati; i dati possono essere stringhe semplici, liste CSV o JSON).
- Risposta:
  - Successo: `OK:<payload>` (il payload può essere vuoto, testo o JSON)
  - Errore: `ERROR:<messaggio>`

Comandi supportati:

1) `VALIDATE_EMAIL:<email>`
   - Esempio richiesta: `VALIDATE_EMAIL:cl16@mail.com`
   - Risposta: `OK:Email valida` oppure `ERROR:Email non esistente`

2) `SEND_EMAIL:<json_email>`
   - JSON `Email` (campi: `id` opzionale, `sender`, `recipients` array, `subject`, `body`, `timestamp` opzionale)
   - Esempio richiesta:
     `SEND_EMAIL:{"sender":"cl16@mail.com","recipients":["mv33@mail.com"],"subject":"Ciao","body":"Test"}`
   - Risposta: `OK:Email inviata con successo` oppure `ERROR: ...` (mittente/destinatari non validi, parse error)

3) `GET_EMAILS:<email>,<fromIndex>`
   - Esempio: `GET_EMAILS:mv33@mail.com,0`
   - Risposta: `OK:[{...}, {...}]` (lista JSON delle nuove email ricevute da `fromIndex`)

4) `GET_SENT_EMAILS:<email>`
   - Esempio: `GET_SENT_EMAILS:cl16@mail.com`
   - Risposta: `OK:[{...}]` (lista JSON delle inviate)

5) `DELETE_EMAIL:<email>,<emailId>,<isSent>`
   - Esempio: `DELETE_EMAIL:cl16@mail.com,7c2b...,true`
   - Risposta: `OK:Email eliminata` oppure `ERROR:Email non trovata`

Note:

- Il server gestisce UNA richiesta per connessione. Se servono più comandi, il client deve aprire più connessioni sequenziali.
- I timestamp JSON usano formato ISO, grazie all’adapter `LocalDateTimeTypeAdapter`.

## Persistenza su disco

Percorso: `maildata/`

- Nome file: sostituisce `@` con `_`, es.: `op81@mail.com` → `op81_mail.com.dat`.
- Contenuto: oggetto serializzato `FileManager.MailboxData` con due liste (`receivedEmails`, `sentEmails`).
- Concorrenza: salvataggi/caricamenti protetti da lock per indirizzo.
- Compatibilità: se trovi una semplice `ArrayList<Email>`, viene interpretata come “ricevute” e “invio” resta vuota.

Quando avviene il salvataggio:

- Su consegna email: salva mittente (inviate) e ciascun destinatario (ricevute).
- Su eliminazione: salva la mailbox dell’utente da cui è stata rimossa l’email.

## Estensioni e manutenzione

Cambiare porta del server:

- Modifica il costruttore in `ServerViewController.initialize`: `new SocketServer(<nuova_porta>, model)`.

Aggiungere nuovi account di default:

- In `ServerModel.initializeDefaultAccounts()`, aggiungi indirizzi nell’array `defaultAccounts` (anche dominio diverso; il file su disco userà `_`).

Aggiungere un nuovo comando al protocollo:

- `ClientHandler.handleRequest`: aggiungi un nuovo `case` con parsing dati e handler dedicato.
- Aggiorna `ServerModel` con la logica di business e `FileManager` se serve persistenza.
- Documenta qui il nuovo comando (richiesta/risposta, esempi).

Migliorare robustezza API:

- Evitare `null` come ritorno in `ServerModel.getNewEmails/getSentEmails`: preferire liste vuote per ridurre i controlli lato client.
- Validazione input: usare `EmailValidator` anche lato protocollo (es. prima di salvare).

Packaging e distribuzione:

- Il jar in `target/MailServer-1.0-SNAPSHOT.jar` non è “fat jar” con JavaFX. Per esecuzione stand-alone senza Maven, servirebbe configurare JavaFX module-path (fuori dallo scopo di questa guida). La via consigliata è usare `mvn javafx:run`.

Test (linee guida):

- Non sono presenti test nel repo. Suggeriti:
  - Unit test su `ServerModel` (consegna, eliminazione, getNewEmails, edge cases destinatari non validi).
  - Test su `FileManager` con file temporanei (Junit `@TempDir`).
  - Test del protocollo con socket loopback (client di test che invia richieste e verifica risposte).

## Troubleshooting (problemi comuni)

Errore “NoClassDefFoundError: Stage” o simili lanciando `java server.ServerApplication`:

- Causa: JavaFX non è nel classpath/module-path. Soluzione: esegui con Maven plugin:
  - `mvn -Djavafx.platform=win javafx:run`

VS Code chiede il path di Maven:

- Imposta: `C:\Users\yasse\Tools\apache-maven-3.9.9\bin\mvn.cmd` in “Maven › Executable: Path”.
- Riavvia VS Code o ricarica la finestra.

Build ok ma la GUI non parte con comandi diversi da Maven:

- Usa sempre il plugin JavaFX per questo progetto; si occupa lui dei moduli JavaFX.

Avvisi su JDK recenti (Jansi/Unsafe warnings):

- Sono warning a runtime di Maven/Jansi con JDK nuovi (es. 24). Non bloccano la build.

Porta in uso (8080):

- Se la porta è occupata, il server non si avvia. Cambia la porta in `ServerViewController` oppure libera la 8080.

Permessi su disco `maildata/`:

- Assicurati che l’utente abbia permessi di lettura/scrittura nella cartella del progetto.

## Riepilogo “come funziona” in 10 righe

1) GUI si avvia con JavaFX.
2) Controller crea modello e avvia server socket su 8080.
3) Ogni connessione crea un handler dedicato.
4) Ogni handler elabora 1 richiesta testuale e chiude.
5) Le email sono oggetti `Email` serializzabili.
6) `ServerModel` è l’orchestratore di business e persistenza.
7) `Mailbox` tiene ricevute e inviate (liste osservabili per la GUI).
8) `FileManager` salva/carica mailbox su file `.dat` con lock.
9) I log sono `ObservableList` aggiornati su thread JavaFX via `Platform.runLater`.
10) Si esegue con `mvn javafx:run` e si osservano i log dalla GUI.

— Fine guida —
