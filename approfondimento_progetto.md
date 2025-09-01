# Approfondimento Tecnico sul Progetto MailSystem

Questo documento fornisce un'analisi tecnica dettagliata dei meccanismi chiave del progetto MailSystem, espandendo la conversazione precedente con un'analisi a livello di codice sorgente. L'obiettivo è spiegare il funzionamento di ogni metodo e il ruolo di ogni variabile nel contesto dell'esecuzione del programma.

## 1. Persistenza dei Dati: Analisi Dettagliata del `FileManager`

La persistenza è interamente gestita dalla classe `server.storage.FileManager`. Analizziamola in dettaglio.

### Variabili di Istanza Chiave

*   `private static final String DATA_DIR = "maildata/";`
    *   **Perché**: Dichiarare la directory dei dati come una costante `static final` la rende una proprietà della classe, non di un'istanza, e ne impedisce la modifica. Questo centralizza la configurazione del percorso in un unico punto, rendendo il codice più pulito e facile da manutenere.

*   `private final Map<String, Lock> userLocks = new ConcurrentHashMap<>();`
    *   **Perché `Map<String, Lock>`**: Questa mappa è il cuore della gestione della concorrenza. Associa un "lucchetto" (`Lock`) a ogni utente (identificato dalla sua email `String`). Questo permette di bloccare l'accesso in modo granulare, solo alla casella di posta interessata, invece di bloccare tutte le operazioni di salvataggio.
    *   **Perché `ConcurrentHashMap`**: La mappa stessa è una risorsa condivisa tra i thread. Un `HashMap` normale non è thread-safe e potrebbe corrompersi se più thread tentano di aggiungervi un lock contemporaneamente. `ConcurrentHashMap` è progettata per l'accesso concorrente e garantisce che le operazioni sulla mappa (come `computeIfAbsent`) siano atomiche e sicure.
    *   **Perché `ReentrantLock`**: Viene usato come implementazione concreta del `Lock`. È una scelta flessibile che permette a un thread di acquisire lo stesso lock più volte senza bloccarsi da solo.

### Metodi Fondamentali

*   `public void saveMailbox(String email, Mailbox mailbox)`
    1.  **`Lock lock = userLocks.computeIfAbsent(email, k -> new ReentrantLock());`**: Questa riga è fondamentale. Cerca un lock per l'email data. Se non esiste (`computeIfAbsent`), ne crea uno nuovo (`new ReentrantLock()`) e lo inserisce nella mappa in modo atomico. Questo previene race condition nella creazione dei lock.
    2.  **`lock.lock();`**: Il thread acquisisce il lock. Da questo momento in poi, nessun altro thread potrà acquisire lo stesso lock (e quindi accedere allo stesso file utente) finché non verrà rilasciato.
    3.  **`try (ObjectOutputStream oos = ...)`**: Il `try-with-resources` garantisce che lo stream `ObjectOutputStream` venga chiuso automaticamente alla fine del blocco, prevenendo resource leak.
    4.  **`MailboxData data = new MailboxData(mailbox.getEmails(), mailbox.getSentEmails());`**: Viene creato un oggetto contenitore `MailboxData` solo con le liste di email. Questo disaccoppia l'oggetto di dominio `Mailbox` (che potrebbe avere campi non serializzabili) dall'oggetto che viene effettivamente scritto su disco.
    5.  **`oos.writeObject(data);`**: L'oggetto `MailboxData` viene serializzato e scritto sul file.
    6.  **`finally { lock.unlock(); }`**: Questo è il passaggio più importante per la correttezza. Il `finally` garantisce che il lock venga **sempre** rilasciato, anche se si verifica un'eccezione durante la scrittura del file. Senza questo, un errore potrebbe lasciare il lock acquisito per sempre, causando un deadlock.

*   `public void loadMailbox(String email, Mailbox mailbox)`
    *   Il meccanismo di locking è identico a `saveMailbox` per garantire una lettura consistente e sicura.
    *   **`try (ObjectInputStream ois = ...)`**: Anche qui, il `try-with-resources` gestisce la chiusura dello stream.
    *   **`MailboxData data = (MailboxData) ois.readObject();`**: L'oggetto viene letto dal file e deserializzato. È necessario un cast esplicito.
    *   **`mailbox.setEmails(data.getReceivedEmails());`**: I dati letti vengono usati per popolare l'oggetto `Mailbox` che è stato passato come parametro.
    *   **`catch (FileNotFoundException e)`**: Questa eccezione viene ignorata volutamente. È un caso normale che il file non esista, ad esempio per un utente appena creato che non ha ancora ricevuto email.

## 2. Gestione della Concorrenza: Oltre il `FileManager`

La mutua esclusione non riguarda solo i file, ma anche le risorse condivise in memoria, come la GUI.

*   **Contesto**: Aggiornamento del Log nella GUI del Server.
*   **Classe Coinvolta**: `server.model.ServerModel`
*   **Variabile**: `private final ObservableList<String> serverLog = FXCollections.observableArrayList();`
    *   **Perché `ObservableList`**: Questa non è una lista comune. È una lista speciale di JavaFX che permette ai componenti della GUI (come una `ListView`) di "osservarla". Ogni volta che un elemento viene aggiunto o rimosso, la lista notifica automaticamente i suoi osservatori, che si aggiornano di conseguenza.
*   **Metodo**: `public void addToLog(String message)`
    *   **Il Problema**: Questo metodo viene chiamato da decine di thread `ClientHandler` diversi, ma deve aggiornare la `serverLog`, che è legata alla GUI. I componenti della GUI di JavaFX **non sono thread-safe**. Qualsiasi tentativo di modificarli da un thread che non sia il "JavaFX Application Thread" risulta in una `IllegalStateException`.
    *   **La Soluzione**: `Platform.runLater(() -> serverLog.add(timestamp + " " + message));`
        *   `Platform.runLater` è un metodo statico di JavaFX che risolve esattamente questo problema. Prende un `Runnable` (in questo caso una lambda) e lo mette in una coda.
        *   Il JavaFX Application Thread processa questa coda ed esegue i `Runnable` uno alla volta, in ordine.
        *   In questo modo, l'aggiunta di un log alla `ObservableList` avviene sempre sul thread corretto, in modo sicuro e ordinato, prevenendo qualsiasi conflitto.

## 3. Connessione Client-Server: Il Ciclo di Vita di una Richiesta

### Lato Server

*   **Classe**: `server.network.SocketServer`
*   **Variabile**: `private volatile boolean running = true;`
    *   **Perché `volatile`**: La variabile `running` è modificata dal thread principale (quando chiama `stop()`) ma è letta dal thread del `SocketServer` nel suo loop. La parola chiave `volatile` garantisce che ogni modifica a questa variabile sia immediatamente visibile a tutti gli altri thread. Senza `volatile`, il thread del server potrebbe continuare a vedere il vecchio valore `true` nella sua cache, e il loop `while(running)` non terminerebbe mai.
*   **Metodo**: `run()`
    *   **`while (running)`**: Il server rimane in un ciclo infinito finché la variabile `running` è `true`.
    *   **`Socket clientSocket = serverSocket.accept();`**: Questa è una chiamata **bloccante**. Il thread si ferma qui, in attesa che un client si connetta. Quando avviene, restituisce un `Socket` che rappresenta quella specifica connessione.
    *   **`new Thread(new ClientHandler(clientSocket, model)).start();`**: Questa è la chiave della concorrenza del server. Invece di gestire la richiesta direttamente (bloccando il server dall'accettare altri client), crea un nuovo oggetto `ClientHandler`, gli passa il socket del client e lo avvia in un **thread completamente nuovo**. Questo permette al loop `run()` di tornare immediatamente a `serverSocket.accept()`, pronto per il prossimo client.

### Lato Client

*   **Classe**: `client.network.ServerConnection`
*   **Metodo**: `private String sendRequest(String request)`
    *   **`try (Socket socket = new Socket(HOST, PORT); ...)`**: Questo `try-with-resources` è il cuore del modello a "connessioni non persistenti". Per ogni singola richiesta, viene creato un **nuovo** oggetto `Socket`, stabilendo una nuova connessione TCP. La sintassi `try-with-resources` garantisce che, alla fine del blocco (sia in caso di successo che di errore), il `socket` (e gli stream associati) venga chiuso automaticamente, prevenendo resource leak.
    *   **`out.println(request);`**: La richiesta testuale viene inviata al server.
    *   **`return in.readLine();`**: Viene letta la singola riga di risposta dal server e restituita al chiamante.
    *   **`catch (IOException e)`**: Se il server non è raggiungibile o la connessione fallisce, viene catturata un'`IOException`. Questo permette al client di gestire lo stato "non connesso".

## 4. Flusso di Caricamento Email al Login: Una Traccia Dettagliata

1.  **`ClientViewController.handleLogin()`**: L'utente preme "Accedi". Il metodo recupera il testo dalla `emailField` e lo mette nella variabile locale `email`. Poi chiama `controller.authenticateUserAsync(email, success -> { ... });`.
    *   **Variabile `success -> { ... }`**: Questa è una lambda che implementa un'interfaccia funzionale (`Consumer<Boolean>`). È una **callback**: un pezzo di codice che viene passato come parametro e che sarà eseguito solo in un secondo momento, quando l'operazione asincrona di autenticazione sarà completata. Questo evita che la GUI si congeli in attesa della risposta dal server.

2.  **`ClientController.authenticateUserAsync(...)`**:
    *   **`new Thread(() -> { ... }).start();`**: Viene creato un nuovo thread in background per non bloccare il thread della GUI.
    *   **`boolean result = model.authenticateUser(email);`**: All'interno del nuovo thread, viene chiamato il metodo del modello che esegue le operazioni di rete bloccanti. La variabile `result` conterrà `true` o `false`.
    *   **`Platform.runLater(() -> callback.accept(result));`**: Una volta ottenuto il risultato, non si può chiamare direttamente la callback, perché questa deve aggiornare la GUI. Si usa `Platform.runLater` per schedulare l'esecuzione della callback sul thread di JavaFX, che a sua volta aggiornerà l'interfaccia (mostrando la schermata principale o un messaggio di errore).

3.  **`ClientModel.authenticateUser(String email)`**:
    *   **`String response = serverConnection.validateEmail(email);`**: Prima chiamata di rete (bloccante).
    *   **`if (response != null && response.startsWith("OK"))`**: Se l'autenticazione ha successo:
        *   **`this.userEmail = email;`**: La variabile di istanza `userEmail` viene impostata. Questa variabile memorizza lo stato della sessione corrente: chi è l'utente loggato.
        *   **`List<Email> received = getEmailsFromServer(...)`**: Seconda chiamata di rete per ottenere l'inbox.
        *   **`List<Email> sent = getSentEmailsFromServer(...)`**: Terza chiamata di rete per la posta inviata.
        *   **`Platform.runLater(() -> { inbox.setAll(received); sentEmails.setAll(sent); });`**: Di nuovo, `Platform.runLater` viene usato per aggiornare le `ObservableList` `inbox` e `sentEmails` sul thread corretto.

4.  **Il "Binding" Magico**:
    *   Nel metodo `ClientViewController.resetSession()`, viene eseguita l'istruzione `inboxListView.setItems(model.getInbox());`.
    *   **`model.getInbox()`** restituisce la `ObservableList` `inbox` dal modello.
    *   Il metodo `setItems` "collega" la `ListView` alla lista. Da questo momento in poi, la `ListView` osserva la lista. Qualsiasi modifica alla lista (come la chiamata `inbox.setAll(received)` vista sopra) viene automaticamente e istantaneamente riflessa nell'interfaccia grafica, senza bisogno di scrivere codice manuale per aggiornare la `ListView`.
