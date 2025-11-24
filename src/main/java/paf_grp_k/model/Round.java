package paf_grp_k.model;

import jakarta.persistence.*;

/**
 * Repräsentiert eine einzelne Runde innerhalb eines Spiels.
 * Enthält Informationen zu den Antworten der Spieler, erreichten Punkten
 * und die zugehörige Frage.
 */
@Entity
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


    // -----------------------------------------------------
    // Getter und Setter mit JavaDoc
    // -----------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }

    public String getAnswerPlayer1() { return answerPlayer1; }
    public void setAnswerPlayer1(String answerPlayer1) { this.answerPlayer1 = answerPlayer1; }

    public String getAnswerPlayer2() { return answerPlayer2; }
    public void setAnswerPlayer2(String answerPlayer2) { this.answerPlayer2 = answerPlayer2; }

    public int getPointsPlayer1() { return pointsPlayer1; }
    public void setPointsPlayer1(int pointsPlayer1) { this.pointsPlayer1 = pointsPlayer1; }

    public int getPointsPlayer2() { return pointsPlayer2; }
    public void setPointsPlayer2(int pointsPlayer2) { this.pointsPlayer2 = pointsPlayer2; }

    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }
}
