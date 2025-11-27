package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Repräsentiert einen Spieler des Systems.
 * Enthält Login-Daten, Statistikwerte und optionale Profildaten.
 */
@Entity
@Getter
@Setter
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
}