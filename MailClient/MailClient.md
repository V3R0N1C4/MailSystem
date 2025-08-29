# Guida completa e didattica al Client di posta elettronica desktop (Java + JavaFX)

Questa guida spiega ogni file, classe e flusso dell’applicazione, evidenzia come tutto interagisce (MVC), valuta se “funziona davvero” e indica miglioramenti pratici. È pensata per preparare un esame.

## Panoramica e architettura (MVC)

- View: FXML + controller JavaFX per UI e interazione.
- Controller: coordina tra UI e Model, senza conoscere la rete.
- Model: stato, sincronizzazioni, invio/elimina, stato connessione; parla con il layer di rete.
- Network: TCP con server locale, protocollo testuale; payload JSON con Gson.

Dipendenze: JavaFX 17.0.15, Gson 2.10.1, Java 11, Maven.

## Flussi principali

1) Avvio: `client.ClientApplication` carica `ClientView.fxml` → `ClientViewController`.
2) Login: format check → `VALIDATE_EMAIL` → se OK, carica inbox+sent.
3) Sync: ogni 5s `GET_EMAILS` da `lastEmailIndex`; ogni 10s test connessione.
4) Componi/Invia: finestra `ComposeView.fxml`, valida destinatari, `SEND_EMAIL`.
5) Azioni email: reply/reply-all/forward/delete con aggiornamento UI.
6) Logout/Chiusura: ferma scheduler e chiude connessione.

## Dettaglio file e classi (con valutazione)

### 1) `client.ClientApplication` (bootstrap JavaFX)

Fa: avvio app, carica FXML, imposta chiusura pulita.

Funziona: sì, solido; `onCloseRequest` chiama `viewController.shutdown()`, poi `Platform.exit()` e `System.exit(0)`.

Suggerimento: `System.exit(0)` è opzionale.

### 2) `client.controller.ClientController`

Fa: espone `authenticateUser`, `sendEmail`, `deleteEmail`, `shutdown`, `getModel` delegando al `ClientModel`.

Valutazione: pulito, separa View e Model.

### 3) `client.model.ClientModel`

Stato: `userEmail`, `inbox`/`sentEmails` (ObservableList), `serverConnection`, `scheduler`, `lastEmailIndex`, `connected`.

Operazioni:

- Autenticazione: valida email → `validateEmail` → se OK carica `inbox(0)` e `sent`, aggiorna UI via `Platform.runLater`, setta `lastEmailIndex`, avvia auto-sync.
- Auto-sync (5s): `getNewEmails(userEmail,lastEmailIndex)` → se ci sono nuove, `inbox.addAll`, `lastEmailIndex=inbox.size()` (su FX thread).
- Check connessione (10s): `connected=testConnection()`; se riconnesso, richiama una sync.
- Invio: se connesso, `sendEmail` → `null` su successo (prefisso `OK`), altrimenti testo errore senza `ERROR:`.
- Eliminazione: `deleteEmail(userEmail,id,isSent)` e rimozione da lista UI.
- Shutdown: `scheduler.shutdown()` e `serverConnection.close()`.

Valutazione: corretta gestione thread/UI; modello reattivo e coerente.

Nota migliorabile: in `checkConnection()`, la chiamata `Platform.runLater(() -> syncWithServer())` esegue rete sul FX thread. Meglio eseguire `syncWithServer()` nel thread scheduler e usare `Platform.runLater` solo per aggiornare le liste.

### 4) `client.model.Email`

Campi: `id` (UUID), `sender`, `recipients`, `subject`, `body`, `timestamp`.

Comportamento: genera `id`/`timestamp` nel costruttore; formatta timestamp; `toString` utile per log.

Valutazione: semplice e corretta.

### 5) `client.model.EmailValidator`

Regex: valida formato standard (TLD 2–7). `null` → false.

Valutazione: adeguata per uso didattico.

### 6) `client.model.LocalDateTimeTypeAdapter`

Fa: serializza/deserializza `LocalDateTime` in ISO.

Criticità: `read(JsonReader in)` usa `in.hasNext()`, non ideale per valori semplici. Migliore versione:

- Se `in.peek()==NULL` → `in.nextNull(); return null;`
- Altrimenti `return LocalDateTime.parse(in.nextString(), formatter);`

### 7) `client.network.ServerConnection`

Config: `localhost:8080`, Gson con adapter; ogni richiesta apre un nuovo `Socket` e legge una riga di risposta.

Comandi:

- `VALIDATE_EMAIL:<email>` → `OK`/`ERROR:<msg>`
- `SEND_EMAIL:<json>` → `OK`/`ERROR:<msg>`
- `GET_EMAILS:<email>,<fromIndex>` → `OK:<jsonArray>`/`ERROR:<msg>`
- `GET_SENT_EMAILS:<email>` → `OK:<jsonArray>`/`ERROR:<msg>`
- `DELETE_EMAIL:<email>,<id>,<isSent>` → `OK`/`ERROR:<msg>`

Valutazione: lineare e chiaro. Suggerito `socket.setSoTimeout(...)` per evitare blocchi se il server non risponde.

### 8) `client.view.ClientViewController`

UI: login box; toolbar (compose/refresh/status/logout); TabPane inbox/sent; pannello dettagli con azioni.

Logica: listener di selezione, reset sessione, updater stato connessione (2s), login su thread separato, apertura compose, delete con conferma, refresh visivo, logout che ferma schedulers e ripristina login.

Valutazione: corretto uso dei thread e di `Platform.runLater`. `EmailListCell` rende bene la lista. Nota: evitare double-brace initialization per micro-stile (non critico).

### 9) `client.view.ComposeViewController`

Funzioni: validazione form live; setup reply/reply-all/forward (destinatari/oggetto/corpo precompilati); invio su thread separato; chiusura su successo.

Valutazione: ben fatto; validazione dei destinatari con regex.

### 10) FXML: `ClientView.fxml`, `ComposeView.fxml`

`ClientView.fxml`: BorderPane → top (login+toolbar) e center (SplitPane con TabPane e dettagli). Bottoni azione e `TextArea` read-only.

`ComposeView.fxml`: GridPane per header (A/Oggetto), `TextArea` corpo, pulsanti “Invia/Annulla”.

Valutazione: coerenti con i controller; namespace JavaFX 11 è compatibile con JavaFX 17.

### 11) Build: `pom.xml`

Java 11, JavaFX, Gson, plugin JavaFX (`mainClass=client.ClientApplication`), profili OS con classifier (`win/mac/linux`).

Esecuzione (Windows, PowerShell):

```powershell
mvn -Pwindows clean javafx:run
```

Per il JAR, il plugin JavaFX è preferibile per gestire correttamente i moduli JavaFX.

## Funziona davvero? Sintesi critica

Sì, se il server rispetta il protocollo (risposte su una riga con prefisso `OK`/`OK:`/`ERROR:` e JSON valido). UI reattiva, auto-sync e gestione offline/online sono implementate in modo corretto.

Migliorie consigliate

- Evitare rete sul FX thread in `checkConnection()` (vedi nota su `syncWithServer`).
- Migliorare `LocalDateTimeTypeAdapter.read(...)` con `peek()/NULL`.
- Impostare `SoTimeout` sui socket per evitare blocchi indefiniti.
- (Opzionale) rendere `handleRefresh` un vero trigger di sync in background.
- (Opzionale) evitare double-brace initialization nelle celle.

Edge case coperti

- Server down: label rosso “Non connesso”, invio/elimina falliscono con messaggi chiari.
- Risposte inattese: funzioni di rete ritornano `null`/vuoto e la UI non crasha.
- Email non valide: bloccate da `EmailValidator`.
- Liste vuote: UI coerente con `ObservableList`.

Contratti rapidi

- Login: email valida → `true/false`; se `true`, `inbox` e `sent` popolati.
- Sync: input `lastEmailIndex` → `List<Email>` nuove; aggiorna `inbox` e indice.
- Invio: `Email` → `null` (OK) o messaggio errore.
- Delete: `Email`, `isSent` → `true/false`.

Prossimi passi (didattici)

- Test unitari su validator, formattazione data, parsing protocollo (mock risposte).
- Notifiche nuove email.
- UX: filtro/ricerca, indicatori caricamento globali.
- Rete: timeouts, retry/backoff, logging.

In sintesi: architettura pulita, flussi chiari. Con 2–3 fix mirati (TypeAdapter, riconnessione, timeout) diventa molto robusto anche in condizioni avverse.
