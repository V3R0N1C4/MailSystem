# MailServer – Audit di Conformità (Programmazione III)

Questo documento verifica la conformità del SOLO progetto Mail Server rispetto alle specifiche fornite. Ogni requisito è marcato con stato (PASS/WARN/FAIL) e, se necessario, con possibili correzioni rapide.

Riferimenti codice principali:
- Entrypoint GUI: `server.ServerApplication`
- Controller GUI: `server.view.ServerViewController`
- Server socket: `server.network.SocketServer`
- Protocollo/handler: `server.controller.ClientHandler`
- Modello e dati: `server.model.*` (`ServerModel`, `Mailbox`, `Email`, `EmailValidator`, `LocalDateTimeTypeAdapter`)
- Persistenza: `server.storage.FileManager`
- FXML GUI: `src/main/resources/server/view/ServerView.fxml`

## 1) Requisiti generali del Mail Server

- Gestione caselle e persistenza su file (txt/binari, no DB)
  - Stato: PASS
  - Evidenza: `FileManager` salva/carica in `maildata/*.dat` (binario) con lock per mailbox e compatibilità retro. `ServerModel` salva su invio/eliminazione.

- Struttura casella: nome account + lista messaggi con ID, mittente, destinatari, oggetto, testo, data
  - Stato: PASS
  - Evidenza: `Mailbox` (ricevute/inviate), `Email` (`id`, `sender`, `recipients`, `subject`, `body`, `timestamp`).

- GUI server con log eventi di interazione client-server
  - Stato: PASS
  - Evidenza: `ServerView.fxml` + `ServerViewController`; log presentati in `ListView`. Log generati in `SocketServer`, `ClientHandler`, `ServerModel`.

- Numero fisso di account precompilati (es. 3); nessuna registrazione da client
  - Stato: PASS
  - Evidenza: `ServerModel.initializeDefaultAccounts()` crea 3 account: `cl16@mail.com`, `mv33@mail.com`, `op81@mail.com`.

## 2) Comunicazione client–server

- JVM separate, comunicazione via socket Java con dati testuali
  - Stato: PASS
  - Evidenza: `ServerSocket`/`Socket` con `BufferedReader`/`PrintWriter` (testo). Nessun riferimento al client.

- Verifica esistenza indirizzi lato server; risposte di errore per indirizzi non esistenti
  - Stato: PASS
  - Evidenza: `ServerModel.isValidEmail`; `ClientHandler` risponde `ERROR` in `VALIDATE_EMAIL`, `SEND_EMAIL` (mittente/destinatari), `GET_*`.

- Nessun socket permanente: una richiesta per connessione
  - Stato: PASS
  - Evidenza: `ClientHandler.run()` elabora UNA riga e chiude.

- Parallelizzazione e mutua esclusione su risorse condivise
  - Stato: PASS
  - Evidenza: thread per connessione; `Mailbox` metodi `synchronized`; `ServerModel` sincronizza operazioni; `FileManager` usa `ReentrantLock` per mailbox.

- Scalabilità: trasferire solo i messaggi non ancora distribuiti
  - Stato: PASS
  - Evidenza: `GET_EMAILS:<email>,<fromIndex>` restituisce solo da `fromIndex` in poi (il client tiene l’indice).

## 3) Architettura e pattern

- MVC con JavaFX; evitare `Observer`/`Observable` deprecati
  - Stato: PASS
  - Evidenza: FXML + Controller; osservabilità con `ObservableList` JavaFX; nessuna classe deprecata usata.

- Log solo eventi di pertinenza del server
  - Stato: PASS
  - Evidenza: connessioni, consegne, errori IO/business; non logga azioni UI del client.

## 4) Protocollo di rete implementato

Formato: richiesta singola riga `COMANDO:DATI`, risposta `OK:...` o `ERROR:...`.
- `VALIDATE_EMAIL:<email>` → `OK:Email valida` | `ERROR:Email non esistente`
- `SEND_EMAIL:<json Email>` → `OK:Email inviata con successo` | `ERROR:...`
- `GET_EMAILS:<email>,<fromIndex>` → `OK:[...]` (lista JSON) | `ERROR:...`
- `GET_SENT_EMAILS:<email>` → `OK:[...]` | `ERROR:...`
- `DELETE_EMAIL:<email>,<emailId>,<isSent>` → `OK:Email eliminata` | `ERROR:Email non trovata`

JSON `Email` gestito con Gson + `LocalDateTimeTypeAdapter` (ISO_LOCAL_DATE_TIME).

## 5) Punti di attenzione / miglioramenti (non bloccanti)

- Invio con destinatari parzialmente validi
  - Stato: WARN
  - Attuale: se esistono destinatari invalidi, risponde `ERROR` e non consegna a quelli validi.
  - Miglioria: consegnare ai validi e restituire esito parziale (elenco invalidi).
  - Dove: `ClientHandler.handleSendEmail` (filtrare destinatari, gestire risposta mista).

- Doppio logging su eliminazione
  - Stato: WARN
  - Attuale: `ClientHandler.handleDeleteEmail` e `ServerModel.deleteEmail` loggano entrambi.
  - Miglioria: lasciare il log di stato al `ServerModel`, ridurre quello in `ClientHandler`.

- Porta hardcoded (8080)
  - Stato: WARN
  - Miglioria: parametrizzare via proprietà di sistema o config.

- Nomi file mailbox: sanitizzazione
  - Stato: WARN
  - Attuale: sostituisce solo `@` con `_`.
  - Miglioria: normalizzare ulteriormente per caratteri non alfanumerici.

- API `ServerModel`: evitare `null` su liste
  - Stato: WARN
  - Attuale: `getNewEmails`/`getSentEmails` possono restituire `null`.
  - Miglioria: tornare liste vuote (robustezza), mantenendo l’handler che valida prima.

## 6) Bug/FAIL bloccanti

- Stato: PASS
- Non sono stati riscontrati bug che impediscano la dimostrazione delle funzionalità richieste.

## 7) Correzioni rapide (indicazioni sintetiche)

- Consegna parziale in `SEND_EMAIL`:
  1. In `ClientHandler.handleSendEmail`, dopo `invalidRecipients`:
     - Se tutti invalidi → `ERROR:Nessun destinatario valido`.
     - Se alcuni validi → `email.setRecipients(validi)`; `model.deliverEmail(email)`; rispondere `OK:Email inviata parzialmente; invalid=[...]`.

- Unificare logging eliminazione:
  1. Rimuovere (o rendere sintetico) il log in `ClientHandler.handleDeleteEmail` e tenere quello in `ServerModel.deleteEmail`.

- Liste vuote al posto di `null` in `ServerModel`:
  1. In `getNewEmails`/`getSentEmails`, sostituire `return null;` con `return Collections.emptyList();` (import `java.util.Collections`).

- Porta configurabile:
  1. In `ServerViewController.initialize`, leggere `int port = Integer.parseInt(System.getProperty("server.port","8080"));` e usare `new SocketServer(port, model)`.

## 8) Note build/esecuzione

- Esecuzione consigliata: `mvn -DskipTests package` e poi `mvn -Djavafx.platform=win javafx:run`.
- L’esecuzione diretta con `java ... server.ServerApplication` fallisce (NoClassDefFoundError: Stage) senza module-path JavaFX: è atteso, usare Maven.

## 9) Conclusione

Il Mail Server rispetta i requisiti principali del progetto (persistenza su file, protocollo testuale su socket non persistenti, gestione account fissi, parallelismo e GUI di log). Le migliorie elencate aumentano robustezza e qualità senza cambiare l’architettura.
