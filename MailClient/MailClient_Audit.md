# MailClient – Audit di Conformità (Programmazione III)

Questo documento verifica la conformità del SOLO progetto Mail Client rispetto alle specifiche fornite in `Progetto.md`. Ogni requisito è marcato con stato (PASS/WARN/FAIL) e, se utile, con possibili correzioni rapide.

Riferimenti codice principali:

- Entrypoint GUI: `client.ClientApplication`
- Controller GUI: `client.view.ClientViewController`, `client.view.ComposeViewController`
- Controller logico: `client.controller.ClientController`
- Modello e dati: `client.model.*` (`ClientModel`, `Email`, `EmailValidator`, `LocalDateTimeTypeAdapter`)
- Rete/protocollo: `client.network.ServerConnection`
- FXML GUI: `src/main/resources/client/view/ClientView.fxml`, `ComposeView.fxml`

## 1) Requisiti generali del Mail Client

- Richiesta e uso dell'indirizzo email all'avvio
  - Stato: PASS
  - Evidenza: login in `ClientView.fxml` (`authBox`); autenticazione in `ClientViewController.handleLogin()` → `ClientController.authenticateUser()`.

- Mantenere email utente e inbox durante l'esecuzione
  - Stato: PASS
  - Evidenza: `ClientModel.userEmail`, `ObservableList<Email> inbox`.

- NON gestire cestino né outbox (solo inbox)
  - Stato: WARN
  - Evidenza: è presente gestione “Inviati” con tab dedicata e `sentEmails` + `GET_SENT_EMAILS`/delete. Extra-funzionalità rispetto al minimo richiesto.
  - Nota: accettabile come estensione, ma se si vuole aderenza stretta: nascondere la tab “Inviati”.

- Client ignaro degli utenti registrati; verifica sintattica via Regex; reinserimento se errato
  - Stato: PASS
  - Evidenza: `EmailValidator` Regex; in login e composizione vengono mostrati errori e richiesto reinserimento.

- Verifica esistenza indirizzi tramite server
  - Stato: PASS
  - Evidenza: `ServerConnection.validateEmail()` su login; il client non possiede elenco utenti.

- Robustezza a server spento; gestione errori e riconnessione automatica
  - Stato: PASS (con WARN di trasparenza)
  - Evidenza: richieste con socket effimere; gestione eccezioni → messaggi `ERROR:`; `checkConnection()` tenta reconnessione periodica; `syncWithServer()` riprende al ripristino.
  - Trasparenza: WARN — in login un errore di connessione è mostrato come “Email non esistente”, vedi 5) Correzioni rapide.

## 2) Funzionalità GUI (solo INBOX richiesto)

- Inserire l’indirizzo email come unica autenticazione
  - Stato: PASS
  - Evidenza: `authBox`, `handleLogin()`.

- Visualizzare lista messaggi e selezionare per dettaglio
  - Stato: PASS
  - Evidenza: `ListView<Email> inboxListView` con cell factory; dettagli mostrati in pannello destro.

- Visualizzare dettagli messaggio
  - Stato: PASS
  - Evidenza: etichette `sender/recipients/subject/date` + `bodyTextArea`.

- Cancellare un messaggio dalla inbox
  - Stato: PASS (con effetto collaterale, vedi 6/7)
  - Evidenza: `ClientController.deleteEmail()` → `ClientModel.deleteEmail(...)` e rimozione dalla lista.

- Creare messaggio con uno o più destinatari; validare sintassi; reinserimento
  - Stato: PASS
  - Evidenza: `ComposeViewController.handleSend()` valida ogni destinatario via `EmailValidator` e mostra alert.

- Reply e Reply-All
  - Stato: PASS
  - Evidenza: `handleReply()`/`handleReplyAll()`; in reply-all esclude l’utente corrente e include il mittente.

- Forward (inoltro)
  - Stato: PASS
  - Evidenza: `handleForward()` con prefisso “Fwd:” e quoting dell’originale.

- Visualizzare stato connessione (connesso/non connesso)
  - Stato: PASS
  - Evidenza: `connectionStatus` aggiornato via scheduler UI sui valori di `ClientModel.isConnected()`.

- Responsività parziale: auto-aggiornamento inbox e notifica arrivo
  - Stato: WARN
  - Evidenza: auto-sync ogni 5s in `ClientModel.startAutoSync()` (OK). Manca notifica esplicita all’arrivo (solo commento TODO).

- Comprensibilità/trasparenza errori
  - Stato: WARN
  - Evidenza: lo scenario “server non raggiungibile” in login è comunicato come “Email non esistente”. Altrove gli errori sono più chiari (es. invio).

## 3) Comunicazione client–server

- JVM separate; socket Java con dati testuali
  - Stato: PASS
  - Evidenza: `ServerConnection` usa `Socket`, `BufferedReader/PrintWriter`, richieste `COMANDO:DATI`.

- Nessun socket permanente: una richiesta per connessione
  - Stato: PASS
  - Evidenza: ogni `sendRequest()` crea e chiude un socket.

- Trasferire solo messaggi non ancora distribuiti (scalabilità)
  - Stato: WARN
  - Evidenza: uso di `GET_EMAILS:<email>,<fromIndex>` con `lastEmailIndex` (OK in principio). Tuttavia `lastEmailIndex` viene ridotto su delete (vedi 6/7) causando possibili duplicati al sync successivo.

## 4) Architettura e pattern

- MVC con JavaFX; evitare `Observer`/`Observable` deprecati
  - Stato: PASS
  - Evidenza: package separati; osservabilità tramite `ObservableList` JavaFX.

- Nessuna comunicazione diretta View ↔ Model (passare sempre dal Controller o via observable appropriati)
  - Stato: WARN
  - Evidenza: la View accede al `ClientModel` (binding liste, `getUserEmail()`, `isConnected()`). Operazioni mutanti passano dal Controller (corretto). Per aderenza stretta, esporre i soli observable/metodi necessari dal Controller.

## 5) Protocollo di rete utilizzato

Formato: richiesta singola riga `COMANDO:DATI`, risposta `OK:...` o `ERROR:...`.

- `VALIDATE_EMAIL:<email>`
- `SEND_EMAIL:<json Email>`
- `GET_EMAILS:<email>,<fromIndex>`
- `GET_SENT_EMAILS:<email>` (extra lato client)
- `DELETE_EMAIL:<email>,<emailId>,<isSent>`

JSON con Gson + `LocalDateTimeTypeAdapter` (ISO_LOCAL_DATE_TIME).

## 6) Punti di attenzione / miglioramenti (non bloccanti)

- Notifica esplicita su nuove email
  - Stato: WARN
  - Miglioria: mostrare un toast/alert/badge quando `newEmails` viene aggiunta (es. `Notifications` ControlsFX o semplice `Alert` non intrusivo).

- Messaggistica d’errore in login
  - Stato: WARN
  - Miglioria: distinguere “server non raggiungibile” da “email non esistente”. Far propagare la differenza da `ServerConnection.validateEmail()` (es. restituire enum/risposta) e mostrarla in UI.

- Port/host configurabili
  - Stato: WARN
  - Miglioria: leggere `System.getProperty("server.host","localhost")` e `System.getProperty("server.port","8080")` in `ServerConnection`.

- Direzione dipendenze View→Model
  - Stato: WARN
  - Miglioria: esporre da `ClientController` i `ObservableList` e lo stato connessione, evitando che la View usi direttamente il Model.

- Adapter LocalDateTime
  - Stato: WARN
  - Nota: l’uso di `JsonReader.hasNext()` in `read()` è atipico; più robusto controllare `in.peek()` contro `JsonToken.NULL` e gestire null correttamente.

## 7) Bug/FAIL rilevanti

- Duplicazione email dopo delete per gestione di `lastEmailIndex`
  - Stato: FAIL
  - Dettaglio: in `ClientModel.deleteEmail(...)` per inbox si fa `lastEmailIndex = inbox.size();`. Questo rende l’indice non monotono: al sync successivo il `fromIndex` può diminuire e il server reinvierà email già distribuite → duplicati in lista.
  - Fix rapido: mantenere un contatore “massimo visto” monotono (es. `nextFetchIndex`) che si aggiorna SOLO quando arrivano nuove email dal server; NON ridurlo su delete locale. In alternativa, deduplicare per `Email.id` alla ricezione.

## 8) Correzioni rapide (indicazioni sintetiche)

- Notifica nuove email:
  1. In `ClientModel.syncWithServer()`, dopo `inbox.addAll(newEmails)`, invocare un callback/UI signal (via Controller) che mostri una notifica con conteggio nuovi messaggi.

- Login: distinguere cause di errore:
  1. Cambiare `ServerConnection.validateEmail` per restituire una risposta completa (es. stringa oppure `enum { OK, NON_ESISTE, CONNESSIONE_KO }`).
  2. In `ClientModel.authenticateUser`, trattare `CONNESSIONE_KO` mostrando “Server non raggiungibile”.

- Indice di sincronizzazione monotono:
  1. Introdurre `private int nextFetchIndex;` nel `ClientModel`.
  2. All’autenticazione: `nextFetchIndex = received.size();`.
  3. In `syncWithServer()`: dopo add, fare `nextFetchIndex += newEmails.size();`.
  4. Rimuovere l’assegnazione a `lastEmailIndex` nel branch delete; non toccare l’indice su eliminazioni locali.

- Parametrizzare host/porta:
  1. In `ServerConnection`, leggere host/porta da `System.getProperty` con default.

- Isolare il Model dalla View:
  1. Esporre in `ClientController` getter per `ObservableList<Email>` e stato connessione; la View non accede più a `model` direttamente.

## 9) Note build/esecuzione

- Esecuzione consigliata: `mvn -DskipTests package` e poi `mvn -Djavafx.platform=win javafx:run`.
- Richiede il server in esecuzione sulla porta configurata.

## 10) Conclusione

Il Mail Client copre tutte le funzionalità principali richieste (login con verifica sintattica e lato server, inbox con dettaglio, delete, compose con multi-destinatari e validazione, reply/reply-all/forward, sincronizzazione automatica e indicatore di connessione) rispettando la comunicazione via socket testuali non persistenti. Restano alcune aree da migliorare: notifica esplicita di nuove email, distinzione chiara degli errori in login, indice di sync monotono per evitare duplicati dopo delete, configurabilità host/porta e lieve rifinitura del rapporto View–Model. L’estensione “Inviati” è extra rispetto al minimo richiesto; va bene, ma può essere nascosta se si richiede aderenza stretta al mandato minimalista.
