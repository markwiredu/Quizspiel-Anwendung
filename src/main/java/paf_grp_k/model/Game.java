package paf_grp_k.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Repräsentiert ein einzelnes Spiel zwischen zwei Spielern.
 * Enthält Informationen über den Status, Start- und Endzeit,
 * Punkte der Spieler sowie den Gewinner.
 */
@Entity
@Table(name = "games")
public class Game {

    /**
     * Eindeutige ID des Spiels (Primärschlüssel).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Status des Spiels (z. B. WAITING, IN_PROGRESS, FINISHED).
     */
    @Enumerated(EnumType.STRING)
    private GameStatus status = GameStatus.WAITING;

    /**
     * Zeitpunkt, an dem das Spiel gestartet wurde.
     */
    private LocalDateTime startTime;

    /**
     * Zeitpunkt, an dem das Spiel beendet wurde.
     */
    private LocalDateTime endTime;

    /**
     * Punktzahl des ersten Spielers.
     */
    private int scorePlayer1 = 0;

    /**
     * Punktzahl des zweiten Spielers.
     */
    private int scorePlayer2 = 0;

    /**
     * Spieler 1 des Spiels.
     */
    @ManyToOne
    @JoinColumn(name = "player1_id")
    private Player player1;

    /**
     * Spieler 2 des Spiels.
     */
    @ManyToOne
    @JoinColumn(name = "player2_id")
    private Player player2;

    /**
     * Gewinner des Spiels (falls bereits ermittelt).
     */
    @ManyToOne
    @JoinColumn(name = "winner_id")
    private Player winner;


    // -----------------------------------------------------
    // Getter und Setter mit JavaDoc
    // -----------------------------------------------------

    /**
     * Gibt die ID des Spiels zurück.
     * @return Spiel-ID
     */
    public Long getId() { return id; }

    /**
     * Setzt die ID des Spiels.
     * @param id neue Spiel-ID
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Gibt den aktuellen Spielstatus zurück.
     * @return Spielstatus
     */
    public GameStatus getStatus() { return status; }

    /**
     * Setzt den Spielstatus.
     * @param status neuer Status (WAITING, IN_PROGRESS, FINISHED)
     */
    public void setStatus(GameStatus status) { this.status = status; }

    /**
     * Gibt die Startzeit des Spiels zurück.
     * @return Startzeit
     */
    public LocalDateTime getStartTime() { return startTime; }

    /**
     * Setzt die Startzeit des Spiels.
     * @param startTime Startzeit
     */
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    /**
     * Gibt die Endzeit des Spiels zurück.
     * @return Endzeit
     */
    public LocalDateTime getEndTime() { return endTime; }

    /**
     * Setzt die Endzeit des Spiels.
     * @param endTime Endzeit
     */
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    /**
     * Gibt die Punktzahl des ersten Spielers zurück.
     * @return Score von Spieler 1
     */
    public int getScorePlayer1() { return scorePlayer1; }

    /**
     * Setzt die Punktzahl des ersten Spielers.
     * @param scorePlayer1 neuer Score von Spieler 1
     */
    public void setScorePlayer1(int scorePlayer1) { this.scorePlayer1 = scorePlayer1; }

    /**
     * Gibt die Punktzahl des zweiten Spielers zurück.
     * @return Score von Spieler 2
     */
    public int getScorePlayer2() { return scorePlayer2; }

    /**
     * Setzt die Punktzahl des zweiten Spielers.
     * @param scorePlayer2 neuer Score von Spieler 2
     */
    public void setScorePlayer2(int scorePlayer2) { this.scorePlayer2 = scorePlayer2; }

    /**
     * Gibt Spieler 1 zurück.
     * @return Spieler 1
     */
    public Player getPlayer1() { return player1; }

    /**
     * Setzt Spieler 1.
     * @param player1 Spieler 1
     */
    public void setPlayer1(Player player1) { this.player1 = player1; }

    /**
     * Gibt Spieler 2 zurück.
     * @return Spieler 2
     */
    public Player getPlayer2() { return player2; }

    /**
     * Setzt Spieler 2.
     * @param player2 Spieler 2
     */
    public void setPlayer2(Player player2) { this.player2 = player2; }

    /**
     * Gibt den Gewinner des Spiels zurück (falls vorhanden).
     * @return Gewinner oder null
     */
    public Player getWinner() { return winner; }

    /**
     * Setzt den Gewinner des Spiels.
     * @param winner Gewinner
     */
    public void setWinner(Player winner) { this.winner = winner; }
}
