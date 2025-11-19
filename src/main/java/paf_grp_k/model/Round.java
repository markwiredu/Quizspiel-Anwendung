package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Repräsentiert eine einzelne Runde innerhalb eines Spiels.
 *
 * <p>Eine Runde verbindet eine gestellte Frage mit den Antworten der beiden Spieler
 * sowie den jeweils erzielten Punkten. Mehrere Runden gehören zu einem
 * übergeordneten Spiel.</p>
 */
@Data
@Entity
@Table(name = "rounds")
public class Round {

    /**
     * Eindeutige ID der Runde.
     *
     * <p>Wird automatisch durch die Datenbank erzeugt (Auto-Increment).</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Die laufende Nummer der Runde im Spiel.
     *
     * <p>Beginnt typischerweise bei 1 und erhöht sich pro Runde.</p>
     */
    private int roundNumber;

    /**
     * Antwort, die Spieler 1 in dieser Runde gegeben hat.
     *
     * <p>Erwartete Werte sind typischerweise „A“, „B“, „C“ oder „D“.</p>
     */
    private String answerPlayer1;

    /**
     * Antwort, die Spieler 2 in dieser Runde gegeben hat.
     *
     * <p>Erwartete Werte sind typischerweise „A“, „B“, „C“ oder „D“.</p>
     */
    private String answerPlayer2;

    /**
     * Punktzahl, die Spieler 1 für diese Runde erhält.
     */
    private int pointsPlayer1 = 0;

    /**
     * Punktzahl, die Spieler 2 für diese Runde erhält.
     */
    private int pointsPlayer2 = 0;

    /**
     * Das Spiel, zu dem diese Runde gehört.
     *
     * <p>Eine Many-to-One-Beziehung — ein Spiel enthält mehrere Runden.</p>
     */
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    /**
     * Die Frage, die in dieser Runde gestellt wurde.
     *
     * <p>Eine Many-to-One-Beziehung — eine Frage kann theoretisch in mehreren Runden verwendet werden.</p>
     */
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}
