# QuizDuell – Kurzanleitung

Diese Anwendung ist ein webbasiertes Quiz-Duell, entwickelt mit Spring Boot und WebSockets.
Die Anwendung ermöglicht Registrierung, Login, Spieler-Matching, ein Quiz-Spiel sowie die Anzeige von Statistiken.

---

## Voraussetzungen

- Java **JDK 17 oder höher**
- IntelliJ IDEA (empfohlen) oder eine andere Java-IDE
- Aktueller Webbrowser (Chrome, Firefox oder Edge)
- Internetverbindung (für CDN-Ressourcen wie Bootstrap)

> Das Projekt wurde mit Java 17 getestet.

---

## Projekt öffnen

1. Repository aus dem Git-Server klonen  
   
2. IntelliJ lädt automatisch die **Gradle-Abhängigkeiten**
3. Warten, bis der Build abgeschlossen ist

---

## Anwendung starten

Die Anwendung ist eine **Spring-Boot-Anwendung**.

### Startklasse

Die Anwendung wird über folgende Klasse gestartet:
>"PafApplication"
> 
>
(diese Klasse enthält die `main`-Methode mit `SpringApplication.run(...)`)

### Start in IntelliJ

1. Datei `PafApplication.java` öffnen
2. Rechtsklick auf die Klasse → **Run 'Application'**
3. Alternativ über die grüne ▶️-Schaltfläche oben rechts starten

Nach erfolgreichem Start erscheint im Terminal u. a.:
>Started Application in ...
>
---

## Anwendung aufrufen

Nach dem Start ist die Anwendung im Browser erreichbar unter:
>http://localhost:8080/index.html
> 
>
Von dort aus kann die gesamte Anwendung bedient werden.

---

## Daten / Beispielspieler



Spieler können einfach erzeugt werden durch:

- Registrierung über die Registrierungsseite  
  (Benutzername, Passwort, optional Profilbild)

Alternativ können bereits angelegte Spieler verwendet werden, falls vorhanden.

Alle Daten werden während der Laufzeit verwaltet.

---

## Funktionaler Ablauf (Überblick)

Die Anwendung unterstützt folgenden Ablauf:

1. Registrierung eines Spielers
2. Login eines Spielers
3. Betreten der Lobby
4. Automatisches Matchmaking
5. Quiz-Spiel (mehrere Runden mit Timer)
6. Punktevergabe und Ergebnisanzeige
7. Anzeige von Statistiken
8. Logout

---

## Screencast

Ein Screencast (Dauer ca. **1–2 Minuten**, MP4) zeigt die Nutzung der Anwendung:

- Registrierung
- Login
- Spieler-Matching
- Quiz-Spiel
- Statistiken
- Logout



 

