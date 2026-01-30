package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO zur Rückgabe von Spielerinformationen über REST-Endpunkte.
 *
 * <p>Diese Klasse wird verwendet, um Spielerdaten als Antwort
 * auf HTTP-Anfragen an den Client zu übertragen.</p>
 *
 * <p>Sie enthält ausschließlich öffentliche, nicht-sensible Informationen
 * und eignet sich für Übersichten, Profile und Statistikanzeigen.</p>
 */
@Getter
@Setter
public class PlayerResponse {

    /**
     * Eindeutige ID des Spielers.
     */
    private Long id;

    /**
     * Benutzername des Spielers.
     */
    private String username;

    /**
     * URL zum Profilbild des Spielers.
     */
    private String profileImageUrl;

    /**
     * Gesamtanzahl der vom Spieler absolvierten Spiele.
     */
    private int totalGames;

    /**
     * Anzahl der vom Spieler gewonnenen Spiele.
     */
    private int gamesWon;

    /**
     * Anzahl der vom Spieler verlorenen Spiele.
     */
    private int gamesLost;

    /**
     * Höchster erreichter Punktestand des Spielers.
     */
    private int highscore;
}
