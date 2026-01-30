package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity-Klasse zur Repräsentation eines Spielers.
 *
 * <p>Ein {@code Player} beschreibt einen registrierten Benutzer des Systems
 * und enthält sowohl Authentifizierungsinformationen als auch statistische
 * Spieldaten.</p>
 *
 * <p>Sensible Informationen wie Passwörter werden ausschließlich
 * in gehashter Form gespeichert und niemals im Klartext persistiert.</p>
 */
@Entity
@Getter
@Setter
@Table(name = "players")
public class Player {

    /**
     * Eindeutige ID des Spielers.
     *
     * <p>Wird automatisch von der Datenbank generiert
     * und dient als Primärschlüssel.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Benutzername des Spielers.
     *
     * <p>Der Benutzername muss eindeutig sein und darf nicht leer sein.</p>
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Gehashter Passwortwert des Spielers.
     *
     * <p>Das ursprüngliche Passwort wird aus Sicherheitsgründen
     * niemals gespeichert oder weitergegeben.</p>
     */
    @Column(nullable = false)
    private String passwordHash;

    /**
     * URL zum Profilbild des Spielers.
     *
     * <p>Kann {@code null} sein, wenn kein Profilbild gesetzt wurde.
     * In diesem Fall kann serverseitig ein Standard-Avatar verwendet werden.</p>
     */
    private String profileImageUrl;

    /**
     * Gesamtanzahl aller vom Spieler absolvierten Spiele.
     */
    private int totalGames = 0;

    /**
     * Anzahl der vom Spieler gewonnenen Spiele.
     */
    private int gamesWon = 0;

    /**
     * Anzahl der vom Spieler verlorenen Spiele.
     */
    private int gamesLost = 0;

    /**
     * Höchster jemals erreichter Punktestand des Spielers.
     */
    private int highscore = 0;

    /**
     * Gesamtanzahl aller vom Spieler erzielten Punkte.
     *
     * <p>Dieser Wert kann zur Ranglisten- oder Statistikberechnung
     * herangezogen werden.</p>
     */
    private int totalPoints;
}
