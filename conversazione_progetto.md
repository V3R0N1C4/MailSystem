# Conversazione sul Progetto MailSystem

## Domanda: nel nostro progetto come viene gestita la persistenza dei dati?

> Nel nostro progetto, la persistenza dei dati è gestita interamente dal **MailServer**. Il client non salva alcuna email in locale, ma le richiede al server ogni volta che ne ha bisogno.
>
> Il server salva i dati in questo modo:
>
> 1.  **Tecnologia**: Utilizza la **serializzazione di oggetti Java** per salvare le caselle di posta su file. Non viene usato un database.
> 2.  **Directory di salvataggio**: Tutti i dati sono memorizzati nella cartella `maildata/` che si trova nella directory principale del server.
> 3.  **Struttura dei file**: Per ogni account di posta elettronica (es. `utente@mail.com`), viene creato un file binario `.dat`. Il nome del file è derivato dall'indirizzo email, sostituendo il carattere `@` con `_` (es. `utente_mail.com.dat`).
> 4.  **Contenuto dei file**: Ogni file contiene un oggetto `MailboxData`, che a sua volta contiene due liste di email: la posta in arrivo (`receivedEmails`) e la posta inviata (`sentEmails`).
> 5.  **Gestione della concorrenza**: Per evitare che più operazioni simultanee possano corrompere i file, l'accesso a ciascun file utente è protetto da un **lock**. In questo modo, se due client tentano di modificare la stessa casella di posta contemporaneamente, le operazioni vengono eseguite in modo sequenziale e sicuro.
> 6.  **Quando avviene il salvataggio**: Il server scrive su file ogni volta che avviene un'operazione che modifica lo stato di una casella di posta, come:
>     *   Alla consegna di una nuova email.
>     *   Dopo l'eliminazione di un'email.
>
> Questo approccio garantisce che i dati siano sempre consistenti e al sicuro sul server.

## Domanda: come viene gestita la mutua esclusione?

> La mutua esclusione nel nostro progetto è gestita in due contesti principali all'interno del **MailServer**:
>
> ### 1. Accesso ai File delle Caselle di Posta
>
> Questa è la parte più critica. Poiché ogni richiesta di un client viene gestita da un thread separato, più thread potrebbero tentare di modificare contemporaneamente lo stesso file sul disco (ad esempio, se due persone inviano un'email allo stesso utente nello stesso momento).
>
> Per evitare la corruzione dei dati, la mutua esclusione è implementata nella classe `server.storage.FileManager` attraverso un meccanismo di **lock per-utente**:
>
> *   **Mappa di Lock**: `FileManager` mantiene una `ConcurrentHashMap<String, Lock>` che associa un oggetto `ReentrantLock` a ogni indirizzo email.
> *   **Acquisizione del Lock**: Prima di eseguire qualsiasi operazione di lettura o scrittura sul file di una casella di posta (es. `utente_mail.com.dat`), il thread acquisisce il lock specifico per quell'utente (`utente@mail.com`).
> *   **Operazione Esclusiva**: Una volta ottenuto il lock, il thread è l'unico ad avere il permesso di accedere a quel file, garantendo che l'operazione sia atomica e sicura.
> *   **Rilascio del Lock**: Al termine dell'operazione, il thread rilascia il lock, permettendo ad altri thread in attesa di accedere alla stessa risorsa.
>
> Questo approccio è efficiente perché blocca l'accesso solo alla singola casella di posta interessata, e non all'intero sistema di file, permettendo ad altri thread di lavorare su caselle di posta differenti senza attese.
>
> ### 2. Aggiornamento della GUI (Log del Server)
>
> Anche l'interfaccia grafica del server è una risorsa condivisa. I vari thread `ClientHandler` devono scrivere messaggi di log sulla `ListView` visualizzata nella finestra del server.
>
> L'aggiornamento diretto della GUI da un thread che non sia il **JavaFX Application Thread** è vietato e causerebbe un'eccezione. La mutua esclusione e la sicurezza dei thread sono garantite in questo modo:
>
> *   **`Platform.runLater()`**: Ogni volta che un thread ha bisogno di aggiungere un messaggio al log, non modifica direttamente la `ObservableList`. Invece, invoca il metodo `Platform.runLater(...)`, passando l'operazione di aggiornamento come un `Runnable`.
> *   **Coda di Eventi**: JavaFX accoda queste operazioni e le esegue una alla volta, in ordine, sul JavaFX Application Thread.
>
> Questo assicura che tutti gli aggiornamenti dell'interfaccia utente avvengano in modo ordinato e sicuro, senza conflitti tra i thread.

## Domanda: come avviene la connessione tra server e client?

> Certamente. La connessione tra client e server è uno degli aspetti chiave del progetto e segue un modello specifico, simile a quello di HTTP/1.0, basato su **connessioni non persistenti**.
>
> Ecco come avviene il processo nel dettaglio:
>
> ### 1. Modello di Connessione: "Una Richiesta, Una Connessione"
>
> La scelta fondamentale è che **non esiste una connessione permanente**. Il client non rimane costantemente collegato al server. Invece, per ogni singola operazione che richiede un'interazione con il server (come validare un'email, inviarne una nuova o chiedere aggiornamenti), il client esegue questo ciclo:
>
> 1.  **Apre** una nuova connessione TCP (un nuovo `Socket`) verso l'indirizzo e la porta del server (`localhost:8080`).
> 2.  **Invia** una singola riga di testo che rappresenta la sua richiesta (es. `VALIDATE_EMAIL:utente@mail.com`).
> 3.  **Attende** e legge la singola riga di risposta dal server (es. `OK:Email valida`).
> 4.  **Chiude** immediatamente la connessione.
>
> Questo modello "short-lived" è semplice da gestire e rende il server più scalabile, poiché non deve mantenere un gran numero di connessioni aperte contemporaneamente.
>
> ### 2. Lato Server: Come viene gestita la richiesta
>
> Sul server, il processo è speculare:
>
> 1.  **Ascolto Continuo**: Un thread principale, gestito dalla classe `SocketServer`, è costantemente in ascolto sulla porta `8080`, in attesa di nuove connessioni.
> 2.  **Accettazione della Connessione**: Quando un client si connette, il `SocketServer` accetta la connessione.
> 3.  **Creazione di un Thread Dedicato**: Per non bloccare l'attesa di altri client, il server non gestisce la richiesta direttamente. Invece, crea un **nuovo thread** (un'istanza di `ClientHandler`) e gli affida il compito di comunicare con quel client specifico.
> 4.  **Gestione della Richiesta**: Il `ClientHandler` legge la richiesta, la elabora (interagendo con il `ServerModel`), invia la risposta e infine chiude la sua connessione con il client. Dopodiché, il thread `ClientHandler` termina il suo ciclo di vita.
>
> ### 3. Protocollo di Comunicazione
>
> La comunicazione avviene tramite **testo semplice**, con un formato di richiesta e risposta molto rigido:
>
> *   **Richiesta del Client**: Sempre una stringa nel formato `COMANDO:DATI`.
>     *   Esempio: `GET_EMAILS:utente@mail.com,0`
> *   **Risposta del Server**: Sempre una stringa che inizia con `OK:` in caso di successo o `ERROR:` in caso di fallimento.
>     *   Esempio: `OK:[{"sender":"...", "subject":"..."}]`
>
> Per dati complessi come un'intera email, l'oggetto viene convertito in una stringa **JSON** (usando la libreria Gson) e inserito come payload nella richiesta o nella risposta.
>
> In sintesi: la connessione è un'interazione breve e mirata, orchestrata dal client per ogni singola operazione, e gestita dal server in modo concorrente grazie a un modello multi-thread.

## Domanda: Come vengono caricate l'email dell'utente nel client, una volta fatto il login?

> Ottima domanda. Il caricamento delle email dopo il login è un processo in più fasi che coinvolge sia il client che il server. Ecco la sequenza esatta degli eventi:
>
> ### 1. Autenticazione e Avvio del Processo
>
> 1.  Dopo che hai inserito l'email e premuto "Accedi", il `ClientViewController` avvia l'autenticazione.
> 2.  Il client invia una richiesta `VALIDATE_EMAIL:<tua_email>` al server.
> 3.  Se il server risponde con `OK`, il client sa che l'utente esiste e può procedere. A questo punto, la logica per caricare le email si attiva all'interno del `ClientModel`.
>
> ### 2. Richiesta delle Email al Server
>
> Subito dopo l'autenticazione, il `ClientModel` esegue **due richieste distinte** al server per popolare sia la posta in arrivo (inbox) sia la posta inviata:
>
> *   **Richiesta per l'Inbox**:
>     *   Il client invia il comando: `GET_EMAILS:<tua_email>,0`
>     *   Il parametro `0` indica al server di inviare **tutte** le email presenti nella casella di posta in arrivo di quell'utente, a partire dal primo messaggio (indice 0).
>
> *   **Richiesta per la Posta Inviata**:
>     *   Il client invia un secondo comando: `GET_SENT_EMAILS:<tua_email>`
>     *   Questo comando chiede al server di restituire l'intera lista dei messaggi inviati da quell'utente.
>
> Il server, per ogni richiesta, recupera le liste di email, le converte in formato **JSON** e le invia al client come risposta.
>
> ### 3. Aggiornamento dell'Interfaccia Utente (UI)
>
> 1.  **Deserializzazione**: Il `ClientModel` riceve le stringhe JSON dal server e le "deserializza", ovvero le riconverte in liste di oggetti `Email` di Java.
>
> 2.  **Popolamento delle `ObservableList`**: Le email ricevute vengono aggiunte a due liste speciali nel `ClientModel`: `inbox` e `sentEmails`. Queste non sono liste normali, ma `ObservableList` di JavaFX.
>
> 3.  **Aggiornamento Automatico della GUI**: La magia delle `ObservableList` sta nel fatto che i componenti grafici (le `ListView` che mostrano le email) sono "collegati" a queste liste. Non appena un nuovo elemento viene aggiunto a una `ObservableList`, la `ListView` corrispondente **si aggiorna automaticamente** per mostrare il nuovo dato, senza bisogno di codice manuale per il refresh. Questo avviene in modo sicuro sul thread della GUI tramite `Platform.runLater`.
>
> ### 4. Avvio della Sincronizzazione Automatica
>
> Una volta completato il caricamento iniziale, il `ClientModel` avvia un **timer** (`ScheduledExecutorService`). Questo timer, ogni 5 secondi, contatterà di nuovo il server con il comando `GET_EMAILS` per chiedere se ci sono *nuovi* messaggi, garantendo che la casella di posta si aggiorni in tempo reale senza che tu debba fare nulla.

## Domanda: Mailbox.java dichiara private ObservableList<Email> emails e sentEmail, perchè? E come vengono usate nel progetto?

> Domanda molto pertinente, che tocca un punto centrale del design del modello. Analizziamo entrambi gli aspetti.
>
> ### 1. Perché `private ObservableList<Email>`?
>
> La dichiarazione può essere scomposta in due parti: `private` e `ObservableList`.
>
> *   **`private`**: Questa è una scelta dettata dal principio dell'**incapsulamento**, uno dei pilastri della programmazione a oggetti. Rendendo le liste `private`, la classe `Mailbox` si assicura di avere il controllo completo sul proprio stato. Nessun'altra classe può accedere direttamente a queste liste per modificarle in modo imprevisto (ad esempio, svuotandole con `list.clear()` o sostituendole). Qualsiasi modifica deve passare attraverso i metodi pubblici che la classe `Mailbox` espone (come `addEmail`, `removeEmail`). Questo permette alla classe di aggiungere logica di controllo, validazione e, come in questo caso, **garantire la thread-safety** usando la parola chiave `synchronized` su questi metodi.
>
> *   **`ObservableList`**: Questa è una scelta di design più specifica. Una `ObservableList` non è una lista standard di Java, ma una classe del framework **JavaFX**. La sua caratteristica principale è che è "osservabile": altre parti del programma possono mettersi in ascolto per essere notificate automaticamente ogni volta che la lista cambia (quando un elemento viene aggiunto, rimosso o modificato).
>     *   **Sul Client**: L'uso di `ObservableList` è **fondamentale**. L'interfaccia grafica (la `ListView` che mostra le email) "osserva" la lista nel `ClientModel`. Quando una nuova email arriva e viene aggiunta alla lista, la `ListView` si aggiorna da sola, magicamente.
>     *   **Sul Server (in `Mailbox.java`)**: Qui la scelta è meno ovvia, perché la GUI del server non visualizza il contenuto delle caselle di posta, ma solo un log. La funzionalità di "osservabilità" non viene quindi sfruttata per la grafica. La ragione più probabile è la **coerenza del modello dati**. Per semplificare lo sviluppo, è molto comune definire le classi del modello (come `Email` e `Mailbox`) una sola volta e poi condividerle o riutilizzarle sia sul client che sul server. In questo caso, il modello è stato probabilmente progettato pensando prima al client (dove `ObservableList` è necessaria) e poi riutilizzato identico sul server. Sebbene sul server si sarebbe potuta usare una `java.util.ArrayList` standard, questa scelta non crea problemi e mantiene il codice del modello consistente tra i due progetti.
>
> ### 2. Come vengono usate nel progetto?
>
> Le liste `emails` e `sentEmails` sono il cuore della stato di una casella di posta in memoria. Ecco il loro ciclo di vita e utilizzo pratico nel server:
>
> 1.  **Inizializzazione**: Quando un oggetto `Mailbox` viene creato, le due liste vengono inizializzate come liste osservabili vuote (`FXCollections.observableArrayList()`).
>
> 2.  **Caricamento da File**: All'avvio del server, il `FileManager` legge i file `.dat` dal disco. Per ogni casella, deserializza le liste di email e le usa per popolare le `ObservableList` in memoria, chiamando i metodi `mailbox.setEmails(...)` e `mailbox.setSentEmails(...)`.
>
> 3.  **Manipolazione Thread-Safe**: Questa è la fase più importante.
>     *   **Aggiunta di Email**: Quando un `ClientHandler` (in un suo thread) processa una richiesta di invio, il `ServerModel` chiama `mailbox.addEmail(...)` per il destinatario e `mailbox.addSentEmail(...)` per il mittente. Poiché questi metodi sono `synchronized`, solo un thread alla volta può modificare le liste di una specifica casella di posta, prevenendo errori di concorrenza.
>     *   **Lettura per il Client**: Quando un client chiede aggiornamenti, il `ServerModel` chiama `mailbox.getNewEmails(fromIndex)`. Anche questo metodo è `synchronized` per garantire che la lista non venga modificata mentre la si sta leggendo per inviarla al client.
>
> 4.  **Salvataggio su File**: Dopo che un'operazione ha modificato una `Mailbox` (es. è stata consegnata una nuova email), il `ServerModel` chiama `FileManager.saveMailbox(...)`. Questo metodo ottiene le liste aggiornate (`mailbox.getEmails()`, `mailbox.getSentEmails()`) e le serializza di nuovo su disco, rendendo la modifica permanente.
>
> In sintesi, all'interno di `Mailbox.java`, queste liste agiscono come il contenitore **in-memoria**, **thread-safe** e **reattivo** dei dati dell'utente, orchestrando lo stato della casella di posta tra le richieste dei client e la persistenza su file.

## Domanda: Spiega nel dettaglio come avviene il refresh della casella di posta degli utenti

> Certamente. Il refresh della casella di posta è un meccanismo di **sincronizzazione automatica e incrementale** che si attiva subito dopo il login. Avviene in background e mantiene la casella di posta dell'utente aggiornata senza che debba premere manualmente alcun pulsante.
>
> Ecco la spiegazione dettagliata del processo, passo dopo passo.
>
> ### 1. Innesco del Meccanismo: l'`ScheduledExecutorService`
>
> *   **Dove**: `client.model.ClientModel`
> *   **Cosa**: Subito dopo che l'autenticazione ha successo e le email iniziali sono state caricate, il `ClientModel` avvia un task schedulato.
> *   **Come**: Utilizza un `ScheduledExecutorService`, un potente strumento di Java per l'esecuzione di operazioni periodiche. Viene chiamato il metodo `scheduleAtFixedRate()`, configurato per eseguire un'operazione specifica **ogni 5 secondi**.
> *   **Perché è importante**: Questo servizio esegue il suo compito su un **thread in background**, separato da quello della GUI. Questo è fondamentale per garantire che l'interfaccia utente rimanga sempre fluida e reattiva, anche mentre il client comunica con il server.
>
> ### 2. L'Operazione di Refresh (eseguita ogni 5 secondi)
>
> Il codice eseguito dal task schedulato è il cuore del meccanismo di refresh. Ecco cosa fa:
>
> 1.  **Calcolo dell'Indice di Sincronizzazione**:
>     *   Il client non chiede ogni volta l'intera casella di posta, sarebbe inefficiente. Invece, calcola l'indice dell'ultimo messaggio che possiede. Questo viene fatto semplicemente controllando la dimensione attuale della lista `inbox`. Ad esempio, se la lista `inbox` contiene 20 email, l'indice sarà 20.
>     *   Questa variabile, che possiamo chiamare `lastEmailIndex`, è la chiave per la **sincronizzazione incrementale**.
>
> 2.  **La Richiesta al Server**:
>     *   Il client invia una richiesta di rete al server nel formato: `GET_EMAILS:<tua_email>,<lastEmailIndex>`
>     *   Esempio: `GET_EMAILS:utente@mail.com,20`
>     *   Questo comando significa: "Per l'utente `utente@mail.com`, dammi tutte le email che hai a partire dalla posizione 20 in poi".
>
> 3.  **La Risposta del Server**:
>     *   Il server riceve la richiesta.
>     *   Accede alla `Mailbox` dell'utente e, usando il metodo `getNewEmails(fromIndex)`, estrae una sotto-lista che contiene solo i messaggi dall'indice 20 fino alla fine.
>     *   Se non ci sono nuove email, questa lista sarà vuota.
>     *   Il server serializza questa lista (vuota o piena) in formato JSON e la invia come risposta: `OK:[...]`.
>
> ### 3. Aggiornamento del Client e della GUI
>
> Questa è la fase finale, dove la "magia" di JavaFX entra in gioco.
>
> 1.  **Ricezione e Deserializzazione**:
>     *   Il `ClientModel` (sempre nel thread in background) riceve la risposta JSON e la converte in una `List<Email>`.
>
> 2.  **Controllo e Aggiornamento della `ObservableList`**:
>     *   Il codice controlla se la lista di nuove email (`newEmails`) non è vuota.
>     *   Se ci sono nuove email, queste devono essere aggiunte alla `ObservableList` `inbox` che è legata alla GUI.
>     *   **Punto cruciale**: Poiché la GUI non può essere modificata da un thread in background, l'operazione di aggiunta viene "impacchettata" in un `Platform.runLater(...)`.
>     *   **`Platform.runLater(() -> inbox.addAll(newEmails));`**: Questo comando dice a JavaFX: "Prendi questa operazione (`inbox.addAll(...)`) e eseguila appena puoi sul thread della GUI".
>
> 3.  **Refresh Automatico della `ListView`**:
>     *   Non appena il thread della GUI esegue `inbox.addAll(newEmails)`, la `ObservableList` notifica i suoi "osservatori".
>     *   L'osservatore principale è la `ListView` nell'interfaccia utente.
>     *   La `ListView` riceve la notifica e **aggiorna automaticamente il suo contenuto**, mostrando le nuove email all'utente in tempo reale, senza che sia stato scritto codice esplicito per ridisegnare la lista.
>
> In parallelo, un altro task schedulato, più semplice, controlla periodicamente lo stato della connessione. Se rileva che una connessione persa è stata ripristinata, può forzare una sincronizzazione per recuperare eventuali email perse durante il periodo di disconnessione, rendendo il sistema ancora più robusto.
