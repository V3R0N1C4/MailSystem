# MailClient – guida completa per l’esame (Java 11, JavaFX 21, Maven)

Questo documento spiega in modo sistematico il client: architettura, classi, flussi, protocollo, threading, UI, build ed edge case. È pensato per rispondere alle domande tipiche d’esame e per accompagnare una demo pratica.

## Architettura in breve (MVC + servizi)

- View: FXML + controller JavaFX per la UI e le interazioni utente.
- Controller: coordina operazioni asincrone e dialoga col Model via callback; non conosce i dettagli di rete.
- Model: stato applicativo (sessione, liste email, connessione), sincronizzazione periodica, invio/elimina; parla col layer di rete.
- Network: TCP client verso server locale (localhost:8080), protocollo testuale a righe; payload JSON con Gson (adattatore per LocalDateTime).

Dipendenze reali (da `pom.xml`): JavaFX 21.0.2, Gson 2.10.1, Java 11, Maven, JavaFX Maven Plugin 0.0.8.

## Diagramma mentale del flusso principale

Avvio → Schermata login → Autenticazione → Caricamento inbox+sent → UI principale (TabPane) →
• Sync automatico (5s) + controllo connessione (10s) → aggiornamento liste
• Azioni: Nuova, Rispondi, Rispondi a tutti, Inoltra, Elimina, Aggiorna, Logout
Chiusura/Logout → arresto scheduler + chiusura connection wrapper

## Classi e responsabilità

### client.ClientApplication (bootstrap JavaFX)

- Carica `ClientView.fxml`, imposta titolo e scena, collega `ClientViewController`.
- onCloseRequest: chiama `viewController.shutdown()`, poi `Platform.exit()` e `System.exit(0)` per una chiusura pulita.

### client.view.ClientViewController (controller della vista principale)

- Orchestrazione sessione: `resetSession()` (nuovo Controller/Model, UI in stato login), `shutdown()` (stop scheduler/controller).
- Binding dati: associa `inboxListView` e `sentListView` alle ObservableList del Model.
- UX: mutua esclusione tra selezioni delle due liste; pulsanti contestuali abilitati in base alla selezione e alla cartella.
- Operazioni:
	- Login: valida formato (EmailValidator), invoca `authenticateUserAsync` del Controller, mostra spinner testuale sul bottone.
	- Stato connessione: scheduler locale ogni 2s aggiorna label (verde/rosso) con `uiManager.updateConnectionStatus(model.isConnected())`.
	- Refresh manuale: invoca `model.syncWithServer()` su thread separato.
	- Apertura compose: nuova/rispondi/rispondi a tutti/inoltra, con precompilazione tramite `ComposeViewController`.
	- Delete: conferma con `DialogManager`, poi `deleteEmailAsync` (gestione sent vs inbox).
	- Logout: `shutdown()` + `resetSession()` (torna al login senza chiudere l’app).

### client.controller.ClientController (coordinamento asincrono)

- Espone metodi async con callback su FX thread: `authenticateUserAsync`, `sendEmailAsync`, `deleteEmailAsync`.
- Incapsula un `ClientModel` e ne espone l’accesso per la UI (getModel). Esegue lavoro pesante off-UI thread e re-boomerang su FX via `Platform.runLater`.

### client.model.ClientModel (stato e logica applicativa)

- Stato: `userEmail`, `inbox` (ObservableList), `sentEmails` (ObservableList), `serverConnection`, `scheduler` (ScheduledExecutorService), `lastEmailIndex`, `connected`.
- Autenticazione:
	- Controllo formato lato View; lato Model richiama `serverConnection.validateEmail(email)`.
	- Se OK: set `userEmail/connected`, carica `received = getNewEmails(email, 0)` e `sent = getSentEmails(email)`, aggiorna ObservableList su FX thread, setta `lastEmailIndex = received.size()`, avvia auto-sync.
- Sync automatica: ogni 5s chiama `syncWithServer()`; recupera nuove email da `lastEmailIndex` e aggiorna inbox (FX thread). Gestisce errori impostando `connected=false`.
- Health-check connessione: ogni 10s `testConnection()`. Se si riconnette, sincronizza.
- Invio: `sendEmail(Email)` → ritorna `null` su OK, altrimenti messaggio di errore senza prefisso `ERROR:`. In caso di OK, il Controller aggiunge ai “Sent”.
- Eliminazione: `deleteEmail(userEmail, id, isSent)` e rimozione dall’ObservableList corrispondente.
- Chiusura: `scheduler.shutdown()` e `serverConnection.close()`.

Nota su threading: gli aggiornamenti alle ObservableList avvengono sempre su FX thread (Platform.runLater). Le chiamate di rete girano fuori dal FX thread (scheduler o thread dedicati dal Controller).

### client.network.ServerConnection (trasporto/serializzazione)

- Connessione per richiesta: ogni API apre un nuovo `Socket(SERVER_HOST, SERVER_PORT)` e scambia una riga (richiesta/risposta).
- Gson con `LocalDateTimeTypeAdapter` per serializzare campi temporali.
- Protocollo a comandi testuali:
	- VALIDATE_EMAIL:`<email>` → risponde `OK` o `ERROR:<motivo>`
	- SEND_EMAIL:`<jsonEmail>` → `OK` o `ERROR:<motivo>`
	- GET_EMAILS:`<email>`,`<fromIndex>` → `OK:<jsonArray>` o `ERROR:<motivo>`
	- GET_SENT_EMAILS:`<email>` → `OK:<jsonArray>` o `ERROR:<motivo>`
	- DELETE_EMAIL:`<email>`,`<id>`,`<isSent>` → `OK` o `ERROR:<motivo>`
- Utilities: `testConnection()` (apre socket e chiude), `close()` placeholder.

Suggerimenti robustezza: impostare `socket.setSoTimeout(...)`; normalizzare risposte `ERROR:`; loggare eccezioni.

### client.view.UIManager (composizione e stati UI)

- Mostra/nasconde macro-sezioni (login, toolbar, SplitPane principale) e aggiorna titolo finestra.
- Aggiorna label stato connessione (verde/rosso), popola/azzera dettagli email, abilita/disable azioni in base al contesto (reply/reply-all disabilitati su cartella “Inviati”).
- Fornisce helper `setLoadingState` per pulsanti (testo e disable).

### client.view.EmailListViewConfigurator (rendering liste)

- Fornisce `configure(ListView, isSentFolder, onEmailSelected)` con cell factory personalizzata e listener di selezione.
- Nella cartella Inviati mostra “A: …” e oggetto; in Inbox mostra “Oggetto” e “Da: …”; timestamp formattato in basso.

### client.view.ComposeViewController (finestra composizione)

- Validazione live dei campi (A/Oggetto/Corpo non vuoti) per abilitare “Invia”.
- Precompilazione:
	- Rispondi: destinatario = mittente originale; “Re: …”; corpo con quote minimale.
	- Rispondi a tutti: destinatari = mittente + tutti i destinatari originali escluso l’utente corrente (evita loop su se stessi).
	- Inoltra: “Fwd: …”; quote del messaggio originale; destinatari vuoti (da compilare).
- Invio asincrono via `ClientController.sendEmailAsync` con feedback UI e chiusura su successo.

### client.model.Email (DTO)

- Campi: `id` (UUID), `sender`, `recipients` (List<String>), `subject`, `body`, `timestamp`.
- Utility: `getFormattedTimestamp()` (dd/MM/yyyy HH:mm), `toString()` per log/lista.

### client.model.EmailValidator (regex)

- Pattern classico username@dominio.tld (TLD 2–7). Metodo statico `isValidEmailFormat(String)`.

### client.model.LocalDateTimeTypeAdapter (Gson)

- Scrive/legge `LocalDateTime` in ISO 8601. Miglioria consigliata per `read(...)`: usare `in.peek()` e gestire `NULL` esplicitamente.

### FXML – struttura UI

- ClientView.fxml: BorderPane
	- top: VBox con login (email + Accedi) e toolbar (Nuova, Aggiorna, stato connessione, Logout)
	- center: SplitPane
		- sinistra: TabPane con “Posta in arrivo” (`inboxListView`) e “Inviati” (`sentListView`)
		- destra: dettagli email (etichette Da/A/Oggetto/Data) + TextArea corpo; azioni Rispondi / Rispondi a Tutti / Inoltra / Elimina
- ComposeView.fxml: GridPane per A/Oggetto, TextArea corpo, HBox con pulsanti Invia/Annulla.

## Flussi dettagliati (con contratti)

Login
- Input: stringa email; pre-validata via `EmailValidator`.
- Passi: `ClientController.authenticateUserAsync(email)` → `ClientModel.authenticateUser(email)` → `ServerConnection.validateEmail`.
- Successo: set sessione, carica inbox(0) e sent, avvia auto-sync, aggiorna titolo finestra “Mail Client – <utente>”.
- Fallimento: dialog ERROR “Email non esistente”, focus sul campo.

Sincronizzazione automatica
- Ogni 5s: `getNewEmails(userEmail, lastEmailIndex)` → se lista non vuota, `inbox.addAll`, aggiorna `lastEmailIndex`.
- Ogni 10s: `testConnection()` e, se riconnessi, esegue una sync.

Composizione e invio
- Validazione destinatari (regex). Costruzione `Email(sender, recipients, subject, body)`.
- `sendEmailAsync`: su OK aggiunge a Sent (Model/Controller) e chiude la finestra; su errore mostra messaggio.

Eliminazione
- Conferma utente, poi `deleteEmailAsync(email, isSent)` → rimozione dall’ObservableList corrispondente e pulizia pannello dettagli.

Refresh manuale
- Invoca `model.syncWithServer()` su thread dedicato. Utile per forzare un pull immediato.

Logout / Chiusura
- Ferma scheduler di ViewController e Model; chiude il wrapper di rete; in logout torna allo stato “login” senza terminare l’app.

## Threading e sicurezza del FX thread

- Rete e I/O: mai sul JavaFX Application Thread. Si usa:
	- Thread dedicati nel Controller per auth/send/delete.
	- Scheduler nel Model per sync/health-check.
- Aggiornamenti UI/ListView: sempre via `Platform.runLater`.
- Nota: in `ClientModel.checkConnection()` l’invocazione a `syncWithServer()` è pianificata tramite `Platform.runLater`. È consigliabile chiamare `syncWithServer()` fuori dal FX thread e usare `Platform.runLater` solo per aggiornare le ObservableList.

## Protocollo e formati

Richieste (una riga) e risposte (una riga):
- OK semplice: `OK`
- OK con payload: `OK:<json>`
- Errore: `ERROR:<messaggio>`

Esempio Email JSON (semplificato):
```json
{
	"id": "<uuid>",
	"sender": "alice@example.com",
	"recipients": ["bob@example.com"],
	"subject": "Ciao",
	"body": "Test",
	"timestamp": "2025-08-31T23:59:59"
}
```

## Build ed esecuzione (Windows PowerShell)

- Requisiti: JDK 11, Maven, accesso a Internet per dipendenze.
- Esecuzione dal modulo MailClient:
```powershell
mvn -Pwindows clean javafx:run
```

Se serve un eseguibile, è preferibile continuare a usare il plugin JavaFX (gestisce i moduli JavaFX); in alternativa si può creare un JAR “thin” e documentare i moduli richiesti a runtime.

## Edge case, limiti e miglioramenti

Coperti
- Server offline: label “Non connesso” (rosso); operazioni di rete falliscono con messaggi chiari; nessun crash.
- Liste vuote o nessuna nuova email: UI coerente, nessuna eccezione.
- Destinatari non validi: blocco invio con dettaglio sull’indirizzo errato.

Limiti attuali
- Ogni richiesta apre un nuovo socket (nessun pooling/keep-alive).
- Nessun timeout esplicito: richieste potrebbero bloccare se il server non risponde.
- `LocalDateTimeTypeAdapter.read` può essere reso più robusto gestendo `NULL` via `peek()`.

Migliorie suggerite (robustezza/UX)
- Impostare `socket.setSoTimeout(… )` e gestire retry/backoff.
- Spostare completamente la sync off-FX thread quando triggerata dal check di riconnessione.
- Notifiche/Badge per nuove email; indicatore caricamento globale durante sync manuale.
- Ricerca/filtro nelle liste; ordinamento personalizzato.
- Logging strutturato delle operazioni di rete.

## Domande tipiche d’esame e spunti

1) Perché usare ObservableList in JavaFX? Come si assicura la modifica sul thread giusto?
- Per aggiornamenti automatici della UI; si usa `Platform.runLater` per mutarle dal FX thread.

2) Come è implementata la sincronizzazione periodica? Quali rischi di concorrenza?
- Con `ScheduledExecutorService`. Rischi: toccare UI fuori dal FX thread; per evitarlo si fa runLater e si serializzano le operazioni.

3) Descrivi il protocollo tra client e server e la gestione degli errori.
- Richieste testuali, risposta `OK`/`OK:<json>`/`ERROR:<msg>`; lato client si normalizza il messaggio rimuovendo `ERROR:` dove serve e si gestisce UI.

4) Come gestite Rispondi a Tutti evitando di includere l’utente tra i destinatari?
- Si filtra la lista dei destinatari originali rimuovendo `currentUser` e si aggiunge il mittente se diverso.

5) Dove collocheresti timeouts e retry e perché?
- Nel layer `ServerConnection` perché è il punto unico d’accesso alla rete; migliora resilienza senza impattare UI/Model.

6) Cosa succede al logout e alla chiusura della finestra?
- Si fermano gli scheduler (View e Model) e si chiude la connessione; al logout si reimposta lo stato UI al login senza terminare l’app.

## Riepilogo per la demo

- Avvio: mostra login → inserire email valida esistente sul server.
- Dopo il login: TabPane Inbox/Inviati, stato connessione aggiornato ogni 2s.
- Comporre, rispondere, inoltrare: mostra precompilazione corretta e invio con feedback.
- Eliminare: mostra conferma e rimozione dalla lista corretta.
- Staccare il server: stato passa a “Non connesso”; riattivandolo la sync riprende.

Con questa architettura, il client è modulare, facilmente estendibile e pronto a domande su MVC, thread-safety, binding JavaFX e gestione di protocolli di rete.
