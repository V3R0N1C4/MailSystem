**MailCommon**: un modulo Java Maven che fornisce classi comuni per gestire email in un'applicazione di posta elettronica.

## Struttura del Progetto

Il progetto è organizzato come un modulo Maven con le seguenti componenti principali:

### 1. **Configurazione Maven (pom.xml)**
Il file `pom.xml` definisce le dipendenze e le configurazioni del progetto, tra cui:
- **Dipendenze**:
  - `gson`: per la serializzazione/deserializzazione JSON

- **Plugin**:
  - `maven-compiler-plugin`: per specificare la versione del JDK (Java 17)
    
- **Proprietà**:
    - `project.build.sourceEncoding`: per impostare la codifica dei file sorgente

### 2. **Modello Email (Email.java)**
Questa è la classe centrale che rappresenta un'email con:
- **Identificativo univoco**: generato automaticamente con UUID
- **Mittente**: indirizzo email del mittente
- **Destinatari**: lista di indirizzi email dei destinatari
- **Oggetto e corpo**: contenuto del messaggio
- **Timestamp**: data e ora di creazione automatica

**Caratteristiche tecniche:**
- Implementa `Serializable` per permettere la serializzazione
- Genera automaticamente ID univoco e timestamp alla creazione
- Fornisce un metodo per formattare il timestamp in formato leggibile (dd/MM/yyyy HH:mm)
- Override del metodo `toString()` per una rappresentazione compatta

### 3. **Validatore Email (EmailValidator.java)**
Classe di utilità per validare il formato degli indirizzi email usando:
- **Espressione regolare** che verifica:
    - Caratteri validi prima della @ (lettere, numeri, alcuni simboli)
    - Presenza obbligatoria della @
    - Dominio valido con estensione da 2 a 7 caratteri
- **Metodo statico** `isValidEmailFormat()` che restituisce true/false
- **Gestione dei null** per evitare errori

### 4. **Adapter per JSON (LocalDateTimeTypeAdapter.java)**
Classe specializzata per la serializzazione/deserializzazione JSON di oggetti `LocalDateTime` con Gson:
- **Serializzazione**: converte LocalDateTime in stringa formato ISO
- **Deserializzazione**: converte stringa JSON in oggetto LocalDateTime
- **Gestione null**: tratta correttamente i valori null
- Utilizza il pattern **TypeAdapter** di Gson per personalizzare la conversione

## Scopo del Modulo

Questo modulo sembra essere progettato come **libreria condivisa** per un sistema di posta elettronica più ampio, fornendo:

1. **Modello dati standardizzato** per le email
2. **Validazione** degli indirizzi email
3. **Serializzazione JSON** compatibile con Gson
4. **Utilità comuni** riutilizzabili in diverse parti dell'applicazione

## Aspetti Tecnici Notevoli

- **Thread-safety**: Le classi di utilità sono stateless e thread-safe
- **Immutabilità parziale**: L'Email ha ID e timestamp immutabili dopo la creazione
- **Serializzazione**: Supporta sia la serializzazione Java standard che JSON
- **Validazione robusta**: Pattern regex ben strutturato per email

Il modulo gestisce le email in un'applicazione Java, in particolare la serializzazione e la validazione dei dati.