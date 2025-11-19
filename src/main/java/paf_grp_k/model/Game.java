package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Repräsentiert ein einzelnes Spiel zwischen zwei Spielern.
 *
 * <p>Diese Klasse wird als JPA-Entity verwendet und entspricht der Tabelle
 * {@code games} in der Datenbank. Sie speichert Informationen über den
 * Spielstatus, die beteiligten Spieler, Start-/Endzeit sowie die erzielten Punkte.</p>
 */
@Data
@Entity
@Table(name = "games")
public class Game {

    /**
     * Eindeutige ID des Spiels.
     *
     * <p>Wird automatisch durch die Datenbank generiert (Auto-Increment).</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Der aktuelle Status des Spiels.
     *
     * <p>Wird als String in der Datenbank gespeichert. Standardwert ist
     * {@link GameStatus#WAITING}.</p>
     */
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;

    /**
     * Zeitpunkt, zu dem das Spiel gestartet wurde.
     *
     * <p>Kann null sein, solange das Spiel noch nicht begonnen hat.</p>
     */
    private LocalDateTime startTime;

    /**
     * Zeitpunkt, zu dem das Spiel beendet wurde.
     *
     * <p>Kann null sein, wenn das Spiel noch läuft oder noch nicht gestartet wurde.</p>
     */
    private LocalDateTime endTime;

    /**
     * Punktestand von Spieler 1.
     */
    private int scorePlayer1 = 0;

    /**
     * Punktestand von Spieler 2.
     */
    private int scorePlayer2 = 0;

    /**
     * Referenz auf den ersten Spieler des Spiels.
     *
     * <p>Wird über eine Many-to-One-Beziehung in der Datenbank abgebildet.</p>
     */
    @ManyToOne
    @JoinColumn(name = "player1_id")
    private Player player1;

    /**
     * Referenz auf den zweiten Spieler des Spiels.
     *
     * <p>Wird über eine Many-to-One-Beziehung in der Datenbank abgebildet.</p>
     */
    @ManyToOne
    @JoinColumn(name = "player2_id")
    private Player player2;

    /**
     * Referenz auf den Gewinner des Spiels.
     *
     * <p>Kann null sein, wenn das Spiel noch nicht beendet ist
     * oder unentschieden endet (falls solche Fälle vorgesehen sind).</p>
     */
    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;
}
