# IngSoft-NotaBene

![Build](https://github.com/A-nto26/IngSoft-NotaBene/actions/workflows/maven.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Tests](https://github.com/A-nto26/IngSoft-NotaBene/actions/workflows/maven.yml/badge.svg?event=push)

Applicazione Spring Boot + HTML/JavaScript per la gestione e condivisione di note.

Descrizione

Nota Bene è una web application per creare, modificare, organizzare e condividere note.

L'applicazione segue un'architettura client-server composta da:

Backend sviluppato con Java e Spring Boot,
organizzato secondo il modello Controller → Service → Repository;

Frontend realizzato con HTML, CSS e JavaScript vanilla;

MapDB come database embedded per la persistenza locale.

Tra le principali funzionalità sono presenti:

+ gestione di note e cartelle;
+ condivisione delle note con diversi livelli di permesso;
+ modifica concorrente tramite sistema di lock;
+ versionamento e ripristino delle note;
+ ricerca e gestione delle note condivise;
+ autenticazione degli utenti.

Tecnologie
+ Java 17
+ Spring Boot 3.3.2
+ Maven
+ MapDB 3.0.9
+ HTML5 / CSS3
+ JavaScript vanilla
+ JUnit 5
+ Mockito
+ GitHub Actions
  
Architettura

Il backend è organizzato in tre livelli:

Controller -> Service -> Repository -> MapDB

I Controller espongono le API REST, i Service gestiscono la logica applicativa e i Repository gestiscono la persistenza tramite MapDB.

Il frontend comunica con il backend tramite API REST e richieste HTTP in formato JSON.

Funzionalità principali
+ Note: creazione, modifica, eliminazione e duplicazione;
+ Condivisione: permessi privata, lettura e scrittura;
+ Versionamento: salvataggio delle versioni precedenti e restore, fino a 50 versioni;
+ Lock system: controllo delle modifiche concorrenti con timeout;
+ Cartelle: creazione, gestione e organizzazione delle note;
+ Autenticazione: registrazione e login con password protette tramite BCrypt;
+ Testing: test unitari con JUnit 5 e Mockito;
+ CI: build e test automatici tramite GitHub Actions.
 
Avvio

Il backend si trova nella cartella backend/.

Compilazione e test

cd backend
mvn clean install

Per eseguire solo i test:

mvn clean test

Avvio del backend

mvn spring-boot:run

Il backend viene avviato su:

http://localhost:8080

Avvio del frontend

Il frontend si trova nella cartella frontend/.

È possibile aprire direttamente auth.html oppure utilizzare un server locale, ad esempio tramite l'estensione Live Server di VS Code.

Persistenza

I dati vengono salvati localmente tramite MapDB:

backend/data/

├── notes.db

└── users.db

Non è necessario configurare un database server esterno.

Documentazione

Per informazioni dettagliate sull'installazione, sulla struttura del progetto, sulle API REST, sulle scelte implementative, sul testing e sul debugging, consultare il Manuale dello Sviluppatore.

Per la documentazione completa del progetto sono inoltre disponibili il Diario del Progetto, la Documentazione Sprint 0 e il Manuale dell'Utente.
