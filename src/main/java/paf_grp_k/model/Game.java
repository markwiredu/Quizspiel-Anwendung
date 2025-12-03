package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Repräsentiert ein einzelnes Spiel zwischen zwei Spielern.
 * Enthält Informationen über den Status, Start- und Endzeit,
 * Punkte der Spieler sowie den Gewinner.
 */
@Entity
@Getter
@Setter
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

    private String category = "ALL";

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
}