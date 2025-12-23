package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Repräsentiert eine einzelne Runde innerhalb eines Spiels.
 * Enthält Informationen zu den Antworten der Spieler, erreichten Punkten
 * und die zugehörige Frage.
 */
@Entity
@Getter
@Setter
@Table(name = "rounds")
public class Round {

    /**
     * Eindeutige ID der Runde (Primärschlüssel).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nummer der Runde innerhalb eines Spiels.
     */
    private int roundNumber;

    /**
     * Antwort von Spieler 1 in dieser Runde.
     */
    private String answerPlayer1;

    /**
     * Antwort von Spieler 2 in dieser Runde.
     */
    private String answerPlayer2;

    /**
     * Punkte, die Spieler 1 in dieser Runde erzielt hat.
     */
    private int pointsPlayer1 = 0;

    /**
     * Punkte, die Spieler 2 in dieser Runde erzielt hat.
     */
    private int pointsPlayer2 = 0;

    /**
     * Startzeit der Runde.
     */
    private LocalDateTime startTime;

    /**
     * Endzeit der Runde (wenn beide Spieler geantwortet haben).
     */
    private LocalDateTime endTime;

    /**
     * Zugehöriges Spiel, zu dem diese Runde gehört.
     */
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    /**
     * Frage, die in dieser Runde gestellt wurde.
     */
    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}