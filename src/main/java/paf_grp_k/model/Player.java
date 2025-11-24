package paf_grp_k.model;

import jakarta.persistence.*;

/**
 * Repräsentiert einen Spieler des Systems.
 * Enthält Login-Daten, Statistikwerte und optionale Profildaten.
 */
@Entity
@Table(name = "players")
public class Player {

    /**
     * Eindeutige ID des Spielers (Primärschlüssel).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Benutzername des Spielers.
     * Muss eindeutig sein und darf nicht leer sein.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Gehashter Passwortwert des Spielers.
     * Das eigentliche Passwort wird aus Sicherheitsgründen nie gespeichert.
     */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * URL zum Profilbild des Spielers.
     * Kann null sein, wenn kein Bild gesetzt wurde.
     */
    private String profileImageUrl;

    /**
     * Gesamtzahl aller gespielten Spiele.
     */
    private int totalGames = 0;

    /**
     * Anzahl der gewonnenen Spiele.
     */
    private int gamesWon = 0;

    /**
     * Anzahl der verlorenen Spiele.
     */
    private int gamesLost = 0;

    /**
     * Bester erzielter Highscore des Spielers.
     */
    private int highscore = 0;


    // -----------------------------------------------------
    // Getter und Setter mit JavaDoc
    // -----------------------------------------------------

    /**
     * Gibt die ID des Spielers zurück.
     * @return Spieler-ID
     */
    public Long getId() { return id; }

    /**
     * Setzt die ID des Spielers.
     * @param id neue Spieler-ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Gibt den Benutzernamen zurück.
     * @return Benutzername
     */
    public String getUsername() { return username; }

    /**
     * Setzt den Benutzernamen.
     * @param username neuer Benutzername
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Gibt den gehashten Passwortwert zurück.
     * @return Passwort-Hash
     */
    public String getPasswordHash() { return passwordHash; }

    /**
     * Setzt den gehashten Passwortwert.
     * @param passwordHash neuer Passwort-Hash
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /**
     * Gibt die Profilbild-URL zurück.
     * @return URL zum Profilbild
     */
    public String getProfileImageUrl() { return profileImageUrl; }

    /**
     * Setzt die Profilbild-URL.
     * @param profileImageUrl neue Bild-URL
     */
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    /**
     * Gibt die Gesamtzahl aller gespielten Spiele zurück.
     * @return Anzahl gespielter Spiele
     */
    public int getTotalGames() { return totalGames; }

    /**
     * Setzt die Gesamtzahl aller gespielten Spiele.
     * @param totalGames neue Gesamtanzahl
     */
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }

    /**
     * Gibt die Anzahl aller gewonnenen Spiele zurück.
     * @return gewonnene Spiele
     */
    public int getGamesWon() { return gamesWon; }

    /**
     * Setzt die Anzahl der gewonnenen Spiele.
     * @param gamesWon neue Anzahl gewonnener Spiele
     */
    public void setGamesWon(int gamesWon) { this.gamesWon = gamesWon; }

    /**
     * Gibt die Anzahl aller verlorenen Spiele zurück.
     * @return verlorene Spiele
     */
    public int getGamesLost() { return gamesLost; }

    /**
     * Setzt die Anzahl der verlorenen Spiele.
     * @param gamesLost neue Anzahl verlorener Spiele
     */
    public void setGamesLost(int gamesLost) { this.gamesLost = gamesLost; }

    /**
     * Gibt den Highscore des Spielers zurück.
     * @return Highscore
     */
    public int getHighscore() { return highscore; }

    /**
     * Setzt den Highscore des Spielers.
     * @param highscore neuer Highscore
     */
    public void setHighscore(int highscore) { this.highscore = highscore; }
}
