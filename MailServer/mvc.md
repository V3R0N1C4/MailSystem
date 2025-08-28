# Architettura MVC: teoria, esempio pratico e mappatura del progetto

Questa nota spiega cos’è l’MVC, perché usarlo, mostra un esempio non legato alle email e mappa i file del progetto nelle giuste responsabilità, affrontando anche il dubbio: “se spostiamo tutto poi non abbiamo niente su controller?”.

---

## Cos’è l’MVC e perché usarlo

- Model: contiene lo stato e la logica di dominio. Non conosce l’interfaccia grafica; espone API/dati a chi li usa.
- View: mostra i dati e invia input utente. Idealmente senza logica di dominio.
- Controller: riceve gli input dalla View, chiama il Model, aggiorna la View. È il “collante” tra i due.

Vantaggi principali:

- Separazione delle responsabilità → codice più semplice da capire e manutenere.
- Testabilità → la logica nel Model si testa senza UI.
- Evoluzione indipendente → cambiare la View non richiede cambiare il Model (e viceversa).

---

## Esempio semplice non email (To‑Do App)

- Model: classi `Task`, `TaskListService` con metodi `addTask`, `completeTask`, `listTasks` (persistenza via `Storage` dedicato).
- View: FXML con ListView e TextField + pulsanti “Aggiungi” e “Completa”.
- Controller: il controller FXML gestisce i click: legge il testo, chiama `TaskListService.addTask(...)`, aggiorna la ListView.

Così la logica (validazioni, regole, salvataggio) sta nel Model/Service; la View si occupa solo di mostrare e inoltrare eventi.

---

## Mappatura MVC nel progetto attuale

- Model (`server.model`)
    - `Email`, `Mailbox`, `EmailValidator`, `LocalDateTimeTypeAdapter`: Modello/utility di dominio.
    - `ServerModel`: Application Model/Service. Gestisce mailbox, delivery, salvataggio tramite `FileManager`, log applicativo.
        - Nota: usa `ObservableList` e `Platform.runLater` per aggiornare il log visto dalla UI. È un accoppiamento tollerabile in app piccola; in una versione più “pura” l’aggiornamento UI starebbe nel controller/view.

- Storage (`server.storage`)
    - `FileManager`: Data access/persistenza, thread‑safe per utente.

- Network (`server.network`)
    - `SocketServer`: Accept loop, crea `ClientHandler` per connessione.
    - (Da riallineare) `ClientHandler` oggi è in `server.controller` ma svolge compiti di rete/protocollo → conviene stia qui.

- View / Controller UI
    - `server.view.ServerViewController`: controller FXML della UI; istanzia `ServerModel`, collega log, avvia/ferma `SocketServer`.
    - `server.controller.ServerController`: duplicato concettuale del precedente (stesse responsabilità UI). Non è referenziato dall’FXML attuale e può essere rimosso o unificato.

In JavaFX è normale che il “Controller” (in senso MVC) coincida con il controller FXML. Quindi non è un problema se non c’è altro nel package `controller` di progetto: il ruolo di Controller è già svolto da `ServerViewController` (oggi sotto `server.view`).

---

## Il dubbio: “Se spostiamo tutto, poi non abbiamo niente su controller?”

Dipende da come vuoi organizzare i package, non dall’architettura:

- Opzione A (consigliata, nessun cambio urgente):
    - Lascia i controller FXML sotto `server.view` (pattern comune in JavaFX).
    - Sposta solo `ClientHandler` in `server.network`.
    - Rimuovi `ServerController` (duplicato). Il package `server.controller` può anche non esistere: il ruolo “C” di MVC è comunque coperto da `ServerViewController`.

- Opzione B (layout alternativo):
    - Sposta `ServerViewController` in `server.controller` (Controller UI separati dal package `view`), lasciando in `view` solo le risorse FXML. Richiede aggiornare `fx:controller` nel file FXML.

- Opzione C (scalabilità futura):
    - Introduci un “ApplicationController” che coordini `ServerModel` e `SocketServer`, mentre il controller FXML delega a lui. Utile in app grandi; non necessario ora.

In tutte le opzioni, l’architettura MVC resta corretta: il Controller esiste (UI controller), anche se sta nel package `view` per convenzione JavaFX.

---

## Proposte di refactoring (documentali, non operative ora)

1) Spostare `ClientHandler` da `server.controller` a `server.network` (è networking/protocollo).
2) Rimuovere o unificare `server.controller.ServerController` con `server.view.ServerViewController` (l’FXML usa il secondo).
3) (Facoltativo) Se vuoi un package `controller` “pieno”, sposta `ServerViewController` lì e tieni in `view` solo FXML.

---

## FAQ rapide su scelte correlate

- Perché sia `Mailbox` sia `MailboxData`?
    - `Mailbox` è il modello runtime con `ObservableList` per la UI; `MailboxData` è DTO serializzabile per lo storage. Separazione utile per non legare la persistenza a JavaFX e mantenere compatibilità.

- Vecchio formato file con una sola lista?
    - In passato si salvavano solo le ricevute (una `List<Email>`). In lettura, se si trova quel formato, le inviate si inizializzano a vuoto per retro‑compatibilità. Quando tutti i file saranno migrati, si potrà rimuovere quel ramo.

- Sovrascrittura file: va bene così?
    - Meglio una scrittura atomica (scrivi su `.tmp` e poi `rename` → operazione atomica) per evitare file parziali in caso di crash. È un miglioramento indipendente dall’MVC.

- Lock per utente: a cosa serve?
    - Garantisce accesso esclusivo per mailbox durante lettura/scrittura su disco, evitando race condition tra thread.

---

## Verdetto attuale

- Architettura per ruoli: buona. Le responsabilità sono separate in Model / View(UI) / Network / Storage.
- Piccoli allineamenti consigliati: spostare `ClientHandler` in `network` e rimuovere `ServerController` duplicato. Il “Controller” MVC rimane il controller FXML (`ServerViewController`), anche se sta nel package `view`.

Questo consente di mantenere il progetto semplice e coerente con le pratiche JavaFX, senza introdurre complessità non necessarie.
