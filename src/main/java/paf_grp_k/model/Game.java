package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity-Klasse zur Repräsentation eines Spiels zwischen zwei Spielern.
 *
 * <p>Ein {@code Game} beschreibt den vollständigen Lebenszyklus eines Spiels –
 * von der Erstellung über den Spielverlauf bis hin zum Abschluss.</p>
 *
 * <p>Die Klasse speichert u. a.:</p>
 * <ul>
 *     <li>den aktuellen Status des Spiels</li>
 *     <li>Start- und Endzeitpunkte</li>
 *     <li>Punktestände beider Spieler</li>
 *     <li>die beteiligten Spieler sowie den Gewinner</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@Table(name = "games")
public class Game {

    /**
     * Eindeutige ID des Spiels.
     *
     * <p>Wird automatisch von der Datenbank generiert
     * und dient als Primärschlüssel.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Aktueller Status des Spiels.
     *
     * <p>Mögliche Werte sind z. B.:</p>
     * <ul>
     *     <li>{@code WAITING} – Spiel wartet auf Start</li>
     *     <li>{@code IN_PROGRESS} – Spiel läuft</li>
     *     <li>{@code FINISHED} – Spiel ist abgeschlossen</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;

    /**
     * Zeitpunkt, an dem das Spiel gestartet wurde.
     *
     * <p>Wird gesetzt, sobald das Spiel von {@code WAITING}
     * in {@code IN_PROGRESS} übergeht.</p>
     */
    private LocalDateTime startTime;

    /**
     * Zeitpunkt, an dem das Spiel beendet wurde.
     *
     * <p>Wird gesetzt, sobald das Spiel abgeschlossen ist.</p>
     */
    private LocalDateTime endTime;

    /**
     * Punktestand des ersten Spielers.
     */
    private int scorePlayer1 = 0;

    /**
     * Punktestand des zweiten Spielers.
     */
    private int scorePlayer2 = 0;

    /**
     * Kategorie bzw. Themengebiet des Spiels.
     *
     * <p>Standardmäßig ist die Kategorie {@code "ALL"},
     * falls keine spezifische Kategorie gewählt wurde.</p>
     */
    private String category = "ALL";

    /**
     * Erster am Spiel beteiligter Spieler.
     */
    @ManyToOne
    @JoinColumn(name = "player1_id")
    private Player player1;

    /**
     * Zweiter am Spiel beteiligter Spieler.
     */
    @ManyToOne
    @JoinColumn(name = "player2_id")
    private Player player2;

    /**
     * Gewinner des Spiels.
     *
     * <p>Dieses Feld ist {@code null}, solange das Spiel
     * noch nicht beendet wurde oder kein Gewinner feststeht.</p>
     */
    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;
}
