package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity-Klasse zur Repräsentation einer einzelnen Spielrunde.
 *
 * <p>Eine {@code Round} gehört genau zu einem {@link Game} und bildet
 * einen abgeschlossenen Abschnitt innerhalb eines Spiels ab.</p>
 *
 * <p>Die Klasse speichert u. a.:</p>
 * <ul>
 *     <li>die Rundennummer</li>
 *     <li>die Antworten beider Spieler</li>
 *     <li>die in dieser Runde erzielten Punkte</li>
 *     <li>Start- und Endzeitpunkte der Runde</li>
 *     <li>die zugehörige Frage</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@Table(name = "rounds")
public class Round {

    /**
     * Eindeutige ID der Runde.
     *
     * <p>Wird automatisch von der Datenbank generiert
     * und dient als Primärschlüssel.</p>
     *
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nummer der Runde innerhalb eines Spiels.
     *
     * <p>Die Zählung beginnt in der Regel bei 1
     * und steigt mit jeder weiteren Runde.</p>
     */
    private int roundNumber;

    /**
     * Antwort des ersten Spielers in dieser Runde.
     */
    private String answerPlayer1;

    /**
     * Antwort des zweiten Spielers in dieser Runde.
     */
    private String answerPlayer2;

    /**
     * In dieser Runde erzielte Punkte des ersten Spielers.
     */
    private int pointsPlayer1 = 0;

    /**
     * In dieser Runde erzielte Punkte des zweiten Spielers.
     */
    private int pointsPlayer2 = 0;

    /**
     * Zeitpunkt, an dem die Runde gestartet wurde.
     */
    private LocalDateTime startTime;

    /**
     * Zeitpunkt, an dem die Runde beendet wurde.
     *
     * <p>Wird typischerweise gesetzt, sobald beide Spieler
     * ihre Antworten abgegeben haben.</p>
     */
    private LocalDateTime endTime;

    /**
     * Spiel, zu dem diese Runde gehört.
     *
     * <p>Mehrere Runden können demselben Spiel zugeordnet sein.</p>
     */
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    /**
     * Frage, die in dieser Runde gestellt wurde.
     *
     * <p>Die Frage definiert die korrekte Antwort
     * und dient als Grundlage für die Punktevergabe.</p>
     */
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}
