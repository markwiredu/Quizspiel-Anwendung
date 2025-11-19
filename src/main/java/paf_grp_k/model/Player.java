package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Repräsentiert einen Spieler im System.
 *
 * <p>Diese Klasse wird als JPA-Entity verwendet und entspricht der Tabelle
 * {@code players} in der Datenbank. Sie speichert grundlegende Informationen
 * über einen Spieler, wie Benutzername, Passwort-Hash, Profilbild und
 * Spielstatistiken.</p>
 */
@Data
@Entity
@Table(name = "players")
public class Player {

    /**
     * Eindeutige ID des Spielers.
     *
     * <p>Wird automatisch durch die Datenbank generiert (Auto-Increment).</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Der eindeutige Benutzername des Spielers.
     *
     * <p>Muss in der Datenbank einzigartig sein und darf nicht null sein.</p>
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Der Hash des Passworts des Spielers.
     *
     * <p>Das Passwort selbst wird nicht gespeichert – nur der Hash. Darf nicht null sein.</p>
     */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * URL zum Profilbild des Spielers.
     *
     * <p>Kann null sein, falls der Spieler kein Profilbild gesetzt hat.</p>
     */
    private String profileImageUrl;

    /**
     * Gesamtanzahl aller gespielten Spiele des Spielers.
     */
    private int totalGames = 0;

    /**
     * Anzahl der Spiele, die der Spieler gewonnen hat.
     */
    private int gamesWon = 0;

    /**
     * Anzahl der Spiele, die der Spieler verloren hat.
     */
    private int gamesLost = 0;

    /**
     * Der persönliche Highscore des Spielers.
     */
    private int highscore = 0;
}

