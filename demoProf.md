# Demo Esame: Domande e Risposte sul Progetto MailSystem

Questo documento simula una discussione di esame sul progetto, fornendo per ogni domanda una risposta "discorsiva" (ideale per una spiegazione verbale) e una "tecnica" (con dettagli sul codice).

---

### Domanda 1: Pattern MVC

**"Avete scelto il pattern Model-View-Controller. Può descrivermi il flusso di dati e di controllo quando l'utente clicca sul pulsante 'Invia' nella finestra di composizione? Quali metodi vengono invocati, in quale ordine, e come comunicano tra loro `ComposeViewController`, `ClientController` e `ClientModel`?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Certamente. Quando l'utente clicca 'Invia', il `ComposeViewController`, che gestisce la finestra di composizione, entra in azione. Per prima cosa, valida i dati inseriti, come l'indirizzo del destinatario e l'oggetto. Se i dati sono validi, il `ComposeViewController` non si occupa direttamente dell'invio. Invece, passa l'oggetto `Email` al `ClientController`.

Il `ClientController` agisce come un mediatore. Prende l'email dal controller della vista e la inoltra al `ClientModel`, che è il cuore della logica applicativa. È il `ClientModel` che sa come comunicare con la rete: si occupa di inviare effettivamente l'email al server.

In questo modo le responsabilità sono ben separate: la View gestisce solo l'interfaccia, il Model contiene la logica di business e la comunicazione di rete, e il Controller fa da ponte tra le due, mantenendo il sistema ordinato e facile da modificare."

#### Risposta Tecnica (Dettagli del codice)

"Il flusso è il seguente:
1.  L'evento `onAction` del pulsante 'Invia' in `ComposeView.fxml` è collegato al metodo `handleSend()` nel `ComposeViewController`.
2.  `handleSend()` recupera i dati dai campi di testo, esegue una validazione tramite `EmailValidator` e, se l'esito è positivo, crea una nuova istanza di `Email`.
3.  A questo punto, invoca `clientController.sendEmail(newEmail)`, passando l'oggetto al controller principale che gli era stato precedentemente iniettato.
4.  Il `ClientController.sendEmail(email)` a sua volta delega la chiamata direttamente al modello: `model.sendEmail(email)`.
5.  È il `ClientModel.sendEmail(email)` che contiene la logica effettiva. Controlla se il client è connesso (`isConnected()`) e poi chiama `serverConnection.sendEmail(email)`, che si occupa della serializzazione in JSON e della comunicazione via socket.
6.  Il metodo `sendEmail` del modello restituisce un valore (un messaggio di errore o `null` in caso di successo) che il `ComposeViewController` può usare per dare un feedback all'utente, come chiudere la finestra o mostrare un alert.
Questo design disaccoppia completamente la logica di rete e di business (`ClientModel`) dalla logica della UI (`ComposeViewController`)."

---

### Domanda 2: Separazione View-Controller

**"Nel client, avete sia un `ClientViewController` che un `ClientController`. Qual è la distinzione di responsabilità tra queste due classi? Perché non avete messo tutta la logica nel `ClientViewController`?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Abbiamo distinto i due ruoli per seguire meglio il principio di singola responsabilità. Il `ClientViewController` è strettamente legato alla vista FXML: il suo unico compito è reagire agli input dell'utente (come i click) e aggiornare i componenti grafici. È il 'controller della vista'.

Il `ClientController`, invece, è un 'controller applicativo'. Non sa nulla dei bottoni o delle finestre, ma orchestra le azioni principali dell'applicazione. Fa da regista, prendendo le richieste dalla vista e coordinando il modello per eseguirle.

Separarli ci ha permesso di avere un `ViewController` più pulito, focalizzato solo sulla UI, e un `Controller` più generale che contiene la logica di alto livello, rendendo il codice più facile da testare e da mantenere."

#### Risposta Tecnica (Dettagli del codice)

"La distinzione segue una variante del pattern MVC, a volte chiamata 'Model-View-Presenter' o 'Application Controller'.
*   **`ClientViewController`**: È il controller nel senso stretto di JavaFX. Le sue responsabilità sono: gestire gli eventi `@FXML`, manipolare direttamente i nodi della scena (spesso delegando a un `UIManager`), e tradurre gli eventi della UI in chiamate a metodi più astratti.
*   **`ClientController`**: Agisce come un *façade* o un coordinatore per il livello di business. Espone metodi come `authenticateUserAsync`, `sendEmail`, `deleteEmail`. Disaccoppia il ciclo di vita dei componenti JavaFX dalla logica di business. Ad esempio, `ClientViewController` viene ricreato se la vista FXML viene ricaricata, ma il `ClientController` e il `ClientModel` potrebbero in teoria avere un ciclo di vita più lungo.

Non inserire la logica nel `ClientViewController` permette di:
1.  **Rispettare il Single Responsibility Principle**: Il `ClientViewController` gestisce la UI, il `ClientController` la logica applicativa.
2.  **Migliorare la Testabilità**: È molto più semplice scrivere test unitari per il `ClientController` e il `ClientModel` perché non hanno dipendenze dirette dal framework JavaFX e dal ciclo di vita della UI."

---

### Domanda 3: Dipendenze nel Modello

**"Ho notato che la classe `Mailbox` sul server utilizza `ObservableList`, che è parte del framework JavaFX. Può giustificare questa scelta di design? Quali sono le implicazioni, in termini di architettura e di disaccoppiamento, nell'avere una dipendenza da una libreria grafica nel modello logico del server?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Questa è un'ottima osservazione. Si tratta di un compromesso tecnico che abbiamo scelto consapevolmente. Usare `ObservableList` nel modello del server non è una scelta 'pura' dal punto di vista architetturale, perché lega la logica di business a una libreria di presentazione.

La ragione è stata principalmente pragmatica e di coerenza: abbiamo sviluppato il modello dati (`Email`, `Mailbox`) pensando prima al client, dove `ObservableList` è fondamentale per l'aggiornamento automatico della grafica. Per evitare di mantenere due versioni leggermente diverse delle stesse classi, abbiamo deciso di riutilizzare lo stesso identico modello anche sul server.

Siamo consapevoli che questo crea una dipendenza non ideale, ma ci ha permesso di accelerare lo sviluppo e di ridurre la duplicazione del codice. Se il progetto dovesse crescere, potremmo considerare di disaccoppiare il modello da JavaFX usando interfacce generiche."

#### Risposta Tecnica (Dettagli del codice)

"La scelta di `ObservableList` in `server.model.Mailbox` introduce una dipendenza diretta dal modulo `javafx.base` nel modello logico del server. Questo costituisce una violazione del principio di 'separazione degli strati' (Layering) in un'architettura rigorosa, dove il dominio del modello non dovrebbe conoscere il framework di presentazione.

Il **trade-off** è stato fatto a favore della **riusabilità del codice** e della **velocità di sviluppo**. L'alternativa sarebbe stata:
1.  Definire nel modello interfacce generiche (es. `java.util.List`).
2.  Usare un'implementazione concreta di `ArrayList` nel server.
3.  Usare un'implementazione `ObservableList` nel client, con la necessità di convertire i dati ricevuti dalla rete.

Questo avrebbe aumentato la complessità. L'impatto negativo della nostra scelta è mitigato dal fatto che il server è comunque un'applicazione JavaFX (per la sua GUI di log) e quindi ha già il runtime di JavaFX disponibile. Tuttavia, lo riconosciamo come un **debito tecnico**: una soluzione non ideale presa per ragioni pratiche."

---

### Domanda 4: Connessioni Non Persistenti

**"La comunicazione tra client e server si basa su connessioni non persistenti (short-lived). Quali sono i pro e i contro di questo approccio rispetto al mantenimento di una connessione persistente per tutta la durata della sessione del client?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Abbiamo scelto connessioni non persistenti, simili a come funziona il web con HTTP/1.0, principalmente per la semplicità e la robustezza del server.

*   **Vantaggi**: Il server è più semplice da realizzare. Non deve tenere traccia di decine di connessioni aperte, risparmiando memoria e risorse. Ogni richiesta è indipendente, quindi se una connessione fallisce non influisce sulle altre. Questo rende il server più robusto e scalabile, perché può servire un gran numero di richieste brevi da molti client diversi senza esaurire le risorse.

*   **Svantaggi**: C'è un costo in termini di performance. Aprire e chiudere una connessione TCP per ogni singola richiesta introduce una latenza (il cosiddetto 'handshake' TCP). Se il client dovesse fare molte richieste in rapida successione, questo overhead diventerebbe significativo. Per il nostro caso d'uso, con richieste non troppo frequenti, abbiamo ritenuto questo svantaggio accettabile."

#### Risposta Tecnica (Dettagli del codice)

"L'approccio a connessioni non persistenti presenta i seguenti trade-off:

*   **Pro**:
    *   **Stateless Server Design**: Il server non deve gestire lo stato delle connessioni. Il `ClientHandler` ha un ciclo di vita brevissimo (legge una riga, scrive una riga, chiude), semplificando la gestione della concorrenza e delle risorse.
    *   **Resource Management**: Evita il problema dell'esaurimento dei 'file descriptor' sul sistema operativo del server in caso di un numero molto elevato di client connessi ma inattivi.
    *   **Robustezza**: Isola i fallimenti. Un errore su una singola connessione non richiede una logica complessa di ripristino dello stato della sessione.

*   **Contro**:
    *   **Latenza**: Ogni richiesta paga il costo del three-way handshake di TCP, che può variare da pochi a centinaia di millisecondi a seconda della rete. Questo è evidente nel `client.network.ServerConnection.sendRequest`, dove un `new Socket()` viene creato per ogni chiamata.
    *   **Overhead CPU e Rete**: La ripetizione dell'handshake e del teardown della connessione consuma più CPU e larghezza di banda rispetto a una connessione persistente.

Una connessione persistente con un protocollo di 'keep-alive' sarebbe stata più performante per un client molto attivo, ma avrebbe richiesto una logica più complessa sul server per gestire i timeout delle connessioni inattive e il multiplexing delle richieste."

---

### Domanda 5: Evoluzione del Protocollo

**"Il vostro protocollo di comunicazione è testuale. Immaginiamo di dover aggiungere una nuova funzionalità, come la possibilità di aggiungere allegati alle email. Come evolvereste il protocollo attuale per supportare l'invio di dati binari?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Il nostro protocollo attuale, che legge una sola riga di testo, non è adatto per i dati binari. Per supportare gli allegati, dovremmo modificarlo in modo significativo.

Una soluzione potrebbe essere quella di adottare un approccio multi-linea. La prima riga conterrebbe ancora il comando, ma potrebbe includere anche la dimensione dei dati binari che seguiranno. Ad esempio: `SEND_EMAIL_WITH_ATTACHMENT:<dimensione_json>,<dimensione_allegato>`. Dopo questa riga, il client invierebbe prima il JSON dell'email, e subito dopo i byte esatti dell'allegato.

Il server leggerebbe la prima riga, saprebbe esattamente quanti byte leggere per il JSON e quanti per l'allegato, e potrebbe così ricostruire l'intera richiesta. Questo è un approccio comune in molti protocolli di rete per gestire payload di dimensioni variabili."

#### Risposta Tecnica (Dettagli del codice)

"Evolvere il protocollo richiederebbe di abbandonare il modello `readLine()`/`println()` per un approccio basato su stream di byte. Ecco una possibile implementazione:

1.  **Modifica del Comando**: Il comando `SEND_EMAIL` verrebbe esteso o sostituito. Ad esempio: `SEND_EMAIL_V2`.
2.  **Header di Lunghezza**: La comunicazione non sarebbe più su una sola riga. Si potrebbe usare un header a lunghezza fissa o un approccio basato su `Content-Length` come in HTTP.
    *   **Esempio**: Il client invia `SEND_EMAIL_V2
Json-Length: 1024
Attachment-Length: 5242880

` seguito da 1024 byte di JSON e 5242880 byte di dati binari.
3.  **Codifica dei Dati Binari**: Per mantenere il protocollo testuale, anche se meno efficiente, si potrebbe codificare l'allegato in **Base64** e includerlo come un campo nel JSON. Questo aumenterebbe la dimensione dei dati di circa il 33% ma non richiederebbe di cambiare la logica di lettura `readLine()`.
    *   **JSON Modificato**: `{"sender": ..., "attachment": {"filename": "doc.pdf", "data": "JVBERi0xLj..."}}`
4.  **Modifica del `ClientHandler`**: Il metodo `run()` nel `ClientHandler` non potrebbe più usare `BufferedReader.readLine()` per l'intera richiesta. Dovrebbe leggere la prima riga per il comando e poi usare `clientSocket.getInputStream().readNBytes(length)` per leggere un numero esatto di byte per il JSON e per l'allegato, basandosi sulle lunghezze specificate nell'header."

---

### Domanda 6: Efficienza del Caricamento Dati

**"Nel `ClientModel`, il caricamento iniziale delle email avviene con due chiamate separate: `GET_EMAILS` e `GET_SENT_EMAILS`. Quali alternative avreste potuto considerare e perché avete scelto questa?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Abbiamo scelto di usare due chiamate separate per semplicità e chiarezza del codice. Mantenere i comandi `GET_EMAILS` e `GET_SENT_EMAILS` separati rende il protocollo più facile da capire e il codice del server più pulito, perché ogni comando ha una singola, chiara responsabilità.

L'alternativa sarebbe stata creare un unico comando, ad esempio `GET_ALL_DATA`, che avrebbe restituito un oggetto JSON più complesso contenente sia la lista della posta in arrivo sia quella della posta inviata. Questo avrebbe ridotto il numero di connessioni di rete da due a una, migliorando leggermente le performance al login grazie alla riduzione della latenza.

Abbiamo ritenuto che il vantaggio in termini di performance di una singola chiamata non fosse così critico da giustificare la maggiore complessità nel JSON e nel codice di gestione della risposta. Abbiamo preferito la soluzione più semplice e leggibile."

#### Risposta Tecnica (Dettagli del codice)

"La scelta di due chiamate separate (`GET_EMAILS` e `GET_SENT_EMAILS`) è un trade-off tra performance di rete e complessità del protocollo.

*   **Approccio Attuale**: Due handshake TCP separati. La latenza di rete viene pagata due volte. Tuttavia, il codice nel `ServerModel` è semplice: `getNewEmails()` e `getSentEmails()` sono due metodi distinti e facili da testare. Le risposte JSON sono semplici array di `Email`.

*   **Alternativa (Singola Chiamata)**:
    *   **Nuovo Comando**: `GET_INITIAL_DATA:<email>`
    *   **Risposta JSON Complessa**: Il server dovrebbe costruire un JSON contenitore, ad esempio:
        ```json
        {
          "inbox": [{"sender":...}, ...],
          "sent": [{"sender":...}, ...]
        }
        ```
    *   **Pro**: Riduzione della latenza di rete totale (un solo round-trip time).
    *   **Contro**: Aumento della complessità. Richiede la creazione di una nuova classe DTO (Data Transfer Object) per mappare questa risposta JSON complessa. Aumenta l'accoppiamento tra le due entità (inbox e sent) che altrimenti sarebbero indipendenti.

La nostra scelta ha privilegiato la **Coesione** e la **Semplicità del Protocollo** rispetto a una micro-ottimizzazione della latenza al login."

---

### Domanda 7: Modello di Concorrenza del Server

**"Il `SocketServer` genera un nuovo thread per ogni connessione client. Quali sono i limiti di questo modello 'un thread per client' in uno scenario con un numero molto elevato di utenti connessi simultaneamente? Quali altri modelli di gestione della concorrenza per server di rete conoscete?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Il nostro modello 'un thread per client' è molto semplice da implementare, ma ha dei limiti di scalabilità. Ogni thread consuma memoria per il suo stack, e il sistema operativo può gestire solo un numero finito di thread. Con migliaia di client connessi, il server esaurirebbe la memoria o passerebbe troppo tempo a cambiare contesto tra i thread (context switching), degradando le performance fino al crash.

Esistono modelli più avanzati. Un'alternativa comune è usare un **pool di thread** (Thread Pool). Invece di creare un nuovo thread ogni volta, si ha un numero fisso di thread pronti a lavorare. Quando arriva una richiesta, viene messa in una coda e uno dei thread del pool la preleva e la esegue. Questo limita il consumo di risorse e migliora le performance sotto carico elevato.

Un approccio ancora più avanzato è l'**I/O non bloccante** (NIO), dove un singolo thread può gestire centinaia di connessioni contemporaneamente, reagendo agli eventi di rete (es. 'dati pronti per essere letti') invece di rimanere bloccato in attesa."

#### Risposta Tecnica (Dettagli del codice)

"Il modello 'thread-per-client' implementato in `SocketServer` (`new Thread(new ClientHandler(...)).start()`) presenta i seguenti limiti:

1.  **Resource Consumption**: Ogni thread ha uno stack di memoria (tipicamente da 256KB a 1MB). 1000 client significherebbero circa 1GB di memoria solo per gli stack.
2.  **Context Switching Overhead**: Il kernel del sistema operativo deve continuamente mettere in pausa e riattivare i thread. Con un numero elevato di thread, questo overhead diventa un collo di bottiglia prestazionale.
3.  **Creation/Destruction Overhead**: Creare e distruggere un thread ha un costo computazionale.

Alternative più scalabili:

*   **Thread Pool**: Implementabile con `java.util.concurrent.ExecutorService` (es. `Executors.newFixedThreadPool(N)`). Le richieste (`ClientHandler`) verrebbero sottomesse al pool (`executor.submit(handler)`). Questo riutilizza i thread e limita il numero massimo di thread attivi, migliorando la stabilità.
*   **Non-Blocking I/O (NIO)**: Utilizzando le classi del package `java.nio` come `ServerSocketChannel`, `Selector` e `ByteBuffer`. Questo è un modello a singolo thread (o pochi thread) basato su un 'event loop'. Un `Selector` monitora più `Channel` (connessioni) e notifica il thread solo quando un'operazione di I/O è effettivamente possibile (es. dati ricevuti). Questo modello è alla base di server ad alte prestazioni come Netty o Vert.x ed è in grado di gestire decine di migliaia di connessioni simultanee con poche risorse."

---

### Domanda 8: Locking Granulare

**"Nella classe `FileManager`, avete implementato un meccanismo di lock granulare, con un lock per ogni casella di posta. Perché questa soluzione è preferibile rispetto a un unico `synchronized` block globale per tutti i salvataggi? In quale scenario il vostro approccio offre un vantaggio prestazionale significativo?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Abbiamo scelto un lock per ogni utente per massimizzare la concorrenza. Se avessimo usato un unico lock globale, ogni volta che il server avesse dovuto salvare i dati di un utente, avrebbe bloccato i salvataggi per *tutti* gli altri utenti. Se l'utente A invia un'email e il server salva la sua casella, l'utente B, che riceve un'email nello stesso momento, dovrebbe aspettare la fine del salvataggio di A.

Con il nostro approccio, invece, il lock è specifico per la casella di posta. Mentre il server salva i dati dell'utente A, può tranquillamente e simultaneamente salvare anche i dati dell'utente B, perché i due lock sono indipendenti. Questo migliora notevolmente le performance in uno scenario con molti utenti attivi che modificano le loro caselle di posta contemporaneamente."

#### Risposta Tecnica (Dettagli del codice)

"L'alternativa di un lock globale sarebbe stata implementata sincronizzando l'intero metodo `saveMailbox` o usando un unico `static final Object lock`. Questo avrebbe serializzato tutte le operazioni di I/O su disco, creando un collo di bottiglia.

Il nostro approccio con `ConcurrentHashMap<String, Lock>` offre un **locking a grana fine (fine-grained locking)**. Il vantaggio prestazionale è evidente in scenari ad alta concorrenza dove le operazioni non contendono la stessa risorsa. Se il Thread 1 chiama `saveMailbox("a@mail.com", ...)` e il Thread 2 chiama `saveMailbox("b@mail.com", ...)`, i due thread otterranno due istanze di `Lock` diverse dalla mappa e potranno eseguire le operazioni di scrittura su file in **parallelo**.

La contesa si verifica solo quando più thread tentano di operare sullo stesso identico utente (es. `saveMailbox("a@mail.com", ...)` e `deleteEmail("a@mail.com", ...)`). In questo caso, il lock garantisce la consistenza dei dati per quella specifica casella, mentre le operazioni su altre caselle non vengono impattate. Questo aumenta significativamente il **throughput** (numero di operazioni al secondo) del sistema di persistenza sotto carico."

---

### Domanda 9: Serializzazione Java

**"La persistenza si basa sulla serializzazione di oggetti Java. Quali sono i rischi di questo approccio, specialmente in termini di manutenibilità a lungo termine? Cosa succederebbe se in futuro modificaste la struttura della classe `Email` o `MailboxData`?"**

#### Risposta Discorsiva (Cosa dire a voce)

"La serializzazione Java è molto comoda, ma anche fragile. Il rischio principale è la manutenibilità. Se in futuro modificassimo una classe serializzata, ad esempio aggiungendo o rimuovendo un campo dalla classe `Email`, rischieremmo di non poter più leggere i vecchi file di dati. Tentando di deserializzare i vecchi dati con la nuova versione della classe, il programma lancerebbe un'eccezione, una `InvalidClassException`.

Per gestire questo problema, Java offre un meccanismo chiamato `serialVersionUID`. È un numero che identifica la versione di una classe. Se lo gestiamo manualmente, possiamo scrivere codice personalizzato per la deserializzazione che sia in grado di gestire diverse versioni dei dati. Tuttavia, è un processo complesso. Per un'applicazione più robusta e a lungo termine, sarebbe stato meglio usare un formato di dati più stabile e flessibile, come JSON, XML o un database."

#### Risposta Tecnica (Dettagli del codice)

"I rischi della serializzazione Java standard sono:

1.  **Brittle Contract**: Il formato binario è strettamente accoppiato alla struttura esatta della classe (nomi dei campi, tipi, gerarchia di classi). Qualsiasi modifica non banale può rompere la compatibilità.
2.  **Versioning**: Se la classe `MailboxData` o `Email` viene modificata (es. `private String subject;` diventa `private Text subject;`), la deserializzazione fallirà con `InvalidClassException` a meno che non si implementi una gestione della versione.
3.  **Gestione della Versione**: Per la compatibilità, si dovrebbe dichiarare esplicitamente un `private static final long serialVersionUID`. Mantenendo lo stesso ID tra le versioni, si indica alla JVM che le classi sono compatibili. Tuttavia, se i campi cambiano, è necessario implementare i metodi `writeObject(ObjectOutputStream)` e `readObject(ObjectInputStream)` per gestire manualmente la serializzazione e deserializzazione, garantendo la compatibilità all'indietro.
4.  **Sicurezza**: La deserializzazione di dati non fidati può portare a vulnerabilità di remote code execution.

Un approccio più robusto per la persistenza avrebbe utilizzato formati di interscambio dati standard:
*   **JSON/Gson**: Salvare i dati come file JSON. È leggibile dall'uomo, meno fragile ai cambiamenti e indipendente dal linguaggio.
*   **Database**: Usare un database embedded come SQLite o H2 con un ORM (Object-Relational Mapping) come JPA/Hibernate per una gestione molto più robusta e scalabile dei dati."

---

### Domanda 10: Bug di Sincronizzazione

**"Il refresh automatico si basa su un indice (`lastEmailIndex`) per scaricare solo le nuove email. Come gestisce il vostro sistema il caso in cui un'email venga eliminata? Questo può creare problemi di sincronizzazione? Se sì, come li risolvereste?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Questa è un'ottima domanda che evidenzia una debolezza nel nostro design attuale. Sì, può creare un problema. Attualmente, se l'utente elimina un'email, la dimensione della lista sul client si riduce. Al refresh successivo, il client invierebbe al server un indice più basso di quello precedente. Il server, vedendo un indice più basso, reinvierebbe email che il client in realtà possiede già, causando la comparsa di duplicati nell'interfaccia.

Per risolverlo, dovremmo smettere di basarci sulla dimensione della lista. Una soluzione robusta sarebbe quella di usare un identificatore che cresce sempre. Ad esempio, il server potrebbe assegnare a ogni email un numero progressivo (un ID numerico crescente) o usare un timestamp molto preciso. Il client memorizzerebbe l'ID o il timestamp dell'ultimo messaggio ricevuto e chiederebbe al server solo i messaggi successivi a quell'identificatore, ignorando completamente le eliminazioni locali ai fini della sincronizzazione."

#### Risposta Tecnica (Dettagli del codice)

"Sì, il design attuale presenta un bug di sincronizzazione. Il problema risiede nel `ClientModel`, dove `lastEmailIndex` viene implicitamente derivato da `inbox.size()`. Se un'email viene eliminata, `inbox.size()` diminuisce, e la successiva richiesta `GET_EMAILS:...,<new_lower_index>` causerà una risincronizzazione di email già presenti, portando a duplicati nella `ObservableList`.

**Soluzioni Proposte**:

1.  **ID Monotonico (Approccio Migliore)**:
    *   **Server**: La classe `Email` dovrebbe avere un campo `long sequenceId` che viene assegnato dal server in modo atomico e strettamente crescente per ogni email che entra nel sistema (indipendentemente dalla casella di posta).
    *   **Client**: Il `ClientModel` memorizzerebbe il `long maxSequenceIdSeen`. La richiesta di refresh diventerebbe `GET_EMAILS_SINCE:<email>,<maxSequenceIdSeen>`.
    *   **Server**: Il `ServerModel` filtrerebbe le email della casella di posta per `sequenceId > maxSequenceIdSeen`.
    *   Questo approccio è robusto contro le eliminazioni e garantisce che un'email venga scaricata una sola volta.

2.  **Deduplicazione sul Client (Workaround)**:
    *   Mantenere l'approccio basato su indice, ma prima di aggiungere le email ricevute alla `ObservableList`, il client dovrebbe controllare se un'email con lo stesso ID (`email.getId()`) è già presente. Si potrebbe usare un `Set<String>` contenente gli ID delle email già in possesso per una verifica efficiente.
    *   Esempio: `List<Email> newEmails = receivedFromServer.stream().filter(e -> !existingIds.contains(e.getId())).collect(Collectors.toList()); inbox.addAll(newEmails);`
    *   Questo è meno efficiente perché scarica dati inutili dalla rete, ma previene i duplicati nella UI."

---

### Domanda 11: Gestione degli Errori

**"Come si comporterebbe il client se il server, a causa di un bug, rispondesse con un JSON malformato o con dati inattesi? Il client andrebbe in crash? Come avete reso robusta la deserializzazione dei dati?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Abbiamo cercato di rendere il client robusto a questo tipo di problemi. Se il server inviasse un JSON non valido, la nostra libreria di deserializzazione, Gson, lancerebbe un'eccezione. Noi abbiamo inserito queste operazioni di deserializzazione all'interno di blocchi `try-catch`.

Quindi, se arriva un JSON rotto, il `try-catch` cattura l'errore, impedisce al client di andare in crash, e la logica di gestione dell'errore può semplicemente ignorare la risposta o, in un'implementazione più avanzata, registrare l'errore per il debug. In ogni caso, l'applicazione continuerebbe a funzionare senza interrompersi bruscamente, garantendo una migliore esperienza utente."

#### Risposta Tecnica (Dettagli del codice)

"La robustezza è gestita a livello del `ClientModel` e del `ServerConnection`. La deserializzazione avviene nel `ClientModel` dopo aver ricevuto la stringa JSON dal `ServerConnection`. Il codice che esegue la conversione è protetto.

**Esempio di codice protetto nel `ClientModel`**:
```java
try {
    Type listType = new TypeToken<ArrayList<Email>>() {}.getType();
    List<Email> newEmails = gson.fromJson(jsonPayload, listType);
    // ... processa newEmails
} catch (JsonSyntaxException e) {
    // Il JSON ricevuto dal server non è valido.
    // Logga l'errore per il debug.
    System.err.println("Errore di sintassi nel JSON ricevuto dal server: " + e.getMessage());
    // Non fare nulla, non aggiornare la lista, non crashare.
}
```

In questo modo, una `JsonSyntaxException` lanciata da `gson.fromJson()` viene catturata. L'esecuzione del metodo di sincronizzazione termina per quel ciclo, ma l'applicazione non va in crash. Il `ScheduledExecutorService` continuerà a eseguire il task di refresh al ciclo successivo, tentando di nuovo la sincronizzazione. Questo rende il client resiliente a errori transitori o a bug del server che producono risposte malformate."

---

### Domanda 12: Debito Tecnico

**"Se potesse tornare indietro, quale parte del progetto progetterebbe in modo diverso? Qual è, secondo lei, la scelta di design più debole o il 'debito tecnico' più significativo che vi siete lasciati alle spalle?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Se potessi tornare indietro, probabilmente investirei più tempo nel disaccoppiare il modello del server da JavaFX, come discusso prima. Anche se la nostra scelta pragmatica ha funzionato, usare `ObservableList` sul server non è ideale e potrebbe creare problemi di manutenibilità in futuro.

Il debito tecnico più significativo, però, è il meccanismo di sincronizzazione basato sulla dimensione della lista. Come abbiamo visto, è soggetto a bug con le eliminazioni. Lo abbiamo lasciato così per semplicità iniziale, ma è la prima cosa che rifattorizzerei in una versione 2.0, implementando un sistema più robusto basato su ID o timestamp crescenti, per garantire che la sincronizzazione sia sempre corretta."

#### Risposta Tecnica (Dettagli del codice)

"Rifattorizzerei due aree principali:

1.  **Disaccoppiamento del Modello Server**: Introdurrei un'interfaccia `Mailbox` nel dominio del modello e userei un'implementazione con `ArrayList` nel server. Questo rimuoverebbe la dipendenza da `javafx.base` e renderebbe il modello più puro e portabile, seguendo i principi di 'Domain-Driven Design'.

2.  **Rifattorizzazione della Sincronizzazione**: Questo è il debito tecnico più critico. Abbandonerei l'approccio basato su `fromIndex` (`List.subList`). Lo sostituirei con un meccanismo basato su un token di continuazione (continuation token). La soluzione più robusta sarebbe:
    *   Aggiungere un campo `long timestamp` o `long sequenceId` alle email, indicizzato nel database/storage del server.
    *   La richiesta del client diventerebbe `GET_EMAILS_SINCE:<email>,<last_timestamp>`.
    *   La query sul server diventerebbe molto più esplicita e corretta: `SELECT * FROM emails WHERE user = ? AND timestamp > ? ORDER BY timestamp ASC`.
    *   Questo risolverebbe il bug della cancellazione e sarebbe molto più efficiente su caselle di posta di grandi dimensioni, poiché sfrutterebbe gli indici del database invece di fare una scansione di una lista in memoria."

---

### Domanda 13: Alternative Tecnologiche

**"Perché avete scelto di usare un `ScheduledExecutorService` per il refresh automatico? Conosce altre alternative offerte da JavaFX, come la classe `Task` o `Service`, per gestire operazioni in background? Quali vantaggi avrebbero potuto offrire?"**

#### Risposta Discorsiva (Cosa dire a voce)

"Abbiamo scelto `ScheduledExecutorService` perché è uno strumento standard di Java, molto potente e flessibile per eseguire compiti ripetuti a intervalli fissi, che era esattamente ciò di cui avevamo bisogno per il refresh ogni 5 secondi.

Siamo a conoscenza delle alternative di JavaFX come `Task` e `Service`. Un `Service` in particolare sarebbe stato un'ottima alternativa. È specificamente progettato per operazioni di lunga durata o periodiche che devono comunicare con la UI. Un `Service` avrebbe incapsulato meglio la logica del nostro 'demone' di sincronizzazione e avrebbe offerto meccanismi più eleganti per gestire lo stato (in esecuzione, fallito, successo) e per aggiornare la UI in modo sicuro, senza dover usare `Platform.runLater` manualmente. Probabilmente, in un'evoluzione del progetto, adotteremmo un `Service` per una migliore integrazione con il ciclo di vita di JavaFX."

#### Risposta Tecnica (Dettagli del codice)

"`ScheduledExecutorService` è una soluzione valida ma agnostica rispetto a JavaFX. Le classi `javafx.concurrent.Task` e `javafx.concurrent.Service` offrono un'integrazione superiore con la piattaforma JavaFX.

*   **`Task<V>`**: È ideale per operazioni in background *una tantum*. Espone proprietà osservabili per `progress`, `message`, `value`, `exception`, che possono essere legate direttamente a componenti della UI (es. `ProgressBar`). Gestisce internamente la transizione tra thread background e thread FX per i suoi metodi `update...()`.

*   **`Service<V>`**: È un `Executor` che esegue `Task`. È riutilizzabile e progettato per operazioni che possono essere avviate, cancellate e riavviate. È la scelta ideale per un'operazione periodica come la nostra. Avremmo potuto creare una classe `EmailSyncService extends Service<List<Email>>`. I vantaggi sarebbero stati:
    1.  **Astrazione dello Stato**: Il `Service` gestisce internamente lo stato del task (READY, SCHEDULED, RUNNING, SUCCEEDED, FAILED, CANCELLED).
    2.  **Migliore Gestione del Threading**: Il `Service` si occupa di creare ed eseguire il `Task` su un thread in background. La logica di aggiornamento della UI verrebbe inserita nei metodi `onSucceeded()` o `onFailed()`, che sono garantiti per essere eseguiti sul JavaFX Application Thread, eliminando la necessità di chiamate esplicite a `Platform.runLater`.
    3.  **Incapsulamento**: Tutta la logica di sincronizzazione (chiamata di rete, deserializzazione) sarebbe contenuta all'interno del metodo `createTask()` del servizio, risultando in un design più pulito e meglio incapsulato rispetto a un `Runnable` anonimo passato a un `ExecutorService`."
