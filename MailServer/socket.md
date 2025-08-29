La stringa viene inviata al client connesso tramite il socket associato a questa istanza di `ClientHandler`.

Dettaglio del processo:
1. Quando il client si connette, il server crea un oggetto `ClientHandler` passando il `Socket` del client.
2. Nel metodo `run()`, viene creato un oggetto `PrintWriter out` associato all'output stream del socket (`clientSocket.getOutputStream()`).
3. Quando il client invia una richiesta, il server la legge e la gestisce tramite il metodo appropriato (ad esempio, `handleDeleteEmail`).
4. All'interno di `handleDeleteEmail`, la stringa di risposta (ad esempio, `"OK:Email eliminata"` o `"ERROR:Email non trovata"`) viene inviata al client tramite `out.println(...)`.
5. Il `PrintWriter` scrive la stringa sull'output stream del socket, che viene ricevuta dal client remoto.

In sintesi, la stringa viene inviata al client che ha aperto la connessione TCP, utilizzando il flusso di output del socket.