## Architettura Generale

Questo è un **mail server** sviluppato in Java con JavaFX che implementa un sistema di posta elettronica con le seguenti caratteristiche principali:

### 1. **Struttura del Progetto**

Il progetto segue il pattern **MVC (Model-View-Controller)** ed è organizzato in questi pacchetti:

- `server.model`: Gestione dei dati (mailbox, email)
- `server.view`: Interfaccia grafica
- `server.controller`: Logica di controllo
- `server.network`: Comunicazione di rete
- `server.storage`: Persistenza dei dati

### 2. **Componenti Principali**

#### **ServerApplication** (Main Class)
- Punto di ingresso dell'applicazione JavaFX
- Carica l'interfaccia grafica dal file FXML
- Gestisce la chiusura pulita del server

#### **ServerModel** (Modello dei Dati)
- Gestisce 3 account email predefiniti: `cl16@mail.com`, `mv33@mail.com`, `op81@mail.com`
- Mantiene una mappa di mailbox per ogni utente
- Gestisce il log degli eventi del server
- Coordina la persistenza dei dati tramite FileManager

#### **Mailbox** (Casella di Posta)
Ogni utente ha una mailbox che contiene:
- **Email ricevute**: Lista delle email in arrivo
- **Email inviate**: Lista delle email spedite
- Metodi per aggiungere, rimuovere e recuperare email

#### **SocketServer** (Server di Rete)
- Ascolta sulla porta **8080**
- Accetta connessioni TCP dai client
- Crea un thread separato per ogni client connesso
- Implementa un modello "una richiesta per connessione" (simile a HTTP)

#### **ClientHandler** (Gestione Client)
Gestisce le richieste dei client attraverso un protocollo testuale:

**Comandi supportati:**
- `VALIDATE_EMAIL`: Verifica se un indirizzo email esiste
- `SEND_EMAIL`: Invia una nuova email
- `GET_EMAILS`: Recupera nuove email ricevute
- `GET_SENT_EMAILS`: Recupera email inviate
- `DELETE_EMAIL`: Elimina una email

#### **FileManager** (Persistenza)
- Salva le mailbox su file usando serializzazione Java
- Ogni utente ha un file `.dat` nella directory `maildata`
- Gestisce l'accesso concorrente ai file tramite lock
- Supporta compatibilità con vecchi formati

### 3. **Funzionalità Dettagliate**

#### **Invio Email**
1. Il client invia il comando `SEND_EMAIL` con l'email in formato JSON
2. Il server verifica che il mittente sia valido
3. Filtra i destinatari validi (scarta quelli inesistenti)
4. Salva l'email nella cartella "Inviati" del mittente
5. Consegna l'email nella cartella "Ricevuti" di ogni destinatario valido
6. Aggiorna i file di persistenza

#### **Ricezione Email**
- Il client chiede le nuove email con `GET_EMAILS:email,indice`
- Il server restituisce solo le email successive all'indice specificato
- Permette sincronizzazione incrementale

#### **Validazione Indirizzi**
- Solo gli indirizzi predefiniti sono considerati validi
- Impedisce l'invio a destinatari inesistenti

#### **Interfaccia Grafica**
- Mostra un log in tempo reale delle attività del server
- Visualizza connessioni, invii, errori
- Interfaccia semplice e pulita in JavaFX

### 4. **Protocollo di Comunicazione**

Il server usa un protocollo testuale semplice:
```
COMANDO:DATI
```

**Risposte:**
- `OK:messaggio` per successo
- `ERROR:messaggio` per errori

**Esempio di sessione:**
```
Client: VALIDATE_EMAIL:user@mail.com
Server: OK:Email valida

Client: SEND_EMAIL:{"sender":"user@mail.com","recipients":["dest@mail.com"],...}
Server: OK:Email inviata con successo
```

### 5. **Gestione della Concorrenza**

- **Thread separato** per ogni client connesso
- **Sincronizzazione** sui metodi del ServerModel
- **Lock per file** per evitare conflitti di scrittura
- **Thread-safe** per l'aggiornamento della GUI

### 6. **Persistenza dei Dati**

- Le email vengono salvate in file binari nella directory `maildata/`
- Ogni utente ha il proprio file: `email_domain.dat`
- Salvataggio automatico ad ogni modifica
- Caricamento all'avvio del server

### 7. **Gestione Errori**

- Validazione completa degli input
- Gestione di destinatari non validi
- Log dettagliato di tutti gli eventi
- Chiusura pulita delle connessioni

Questo mail server è progettato per essere semplice ma robusto, con un'architettura che separa chiaramente le responsabilità e garantisce la persistenza dei dati.