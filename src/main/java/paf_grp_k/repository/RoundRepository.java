package paf_grp_k.repository;

import paf_grp_k.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoundRepository extends JpaRepository<Round, Long> {

    /**
     * Finde alle Runden eines Spiels
     */
    List<Round> findByGameId(Long gameId);

    /**
     * Finde Runde nach Spiel-ID und Runden-Nummer
     */
    Optional<Round> findByGameIdAndRoundNumber(Long gameId, int roundNumber);

    /**
     * Finde die aktuelle Runde eines Spiels (höchste Runden-Nummer)
     */
    @Query("SELECT r FROM Round r WHERE r.game.id = :gameId ORDER BY r.roundNumber DESC LIMIT 1")
    Optional<Round> findCurrentRoundByGameId(@Param("gameId") Long gameId);

    /**
     * Prüfe ob eine Runde für Spiel und Runden-Nummer existiert
     */
    boolean existsByGameIdAndRoundNumber(Long gameId, int roundNumber);

    /**
     * Finde Runden wo ein bestimmter Spieler noch nicht geantwortet hat
     */
    @Query("SELECT r FROM Round r WHERE r.game.id = :gameId AND " +
            "(:playerNumber = 1 AND r.answerPlayer1 IS NULL) OR " +
            "(:playerNumber = 2 AND r.answerPlayer2 IS NULL)")
    List<Round> findRoundsWithMissingAnswer(@Param("gameId") Long gameId,
                                            @Param("playerNumber") int playerNumber);

    /**
     * Zähle wie viele Runden in einem Spiel bereits gespielt wurden
     */
    @Query("SELECT COUNT(r) FROM Round r WHERE r.game.id = :gameId")
    int countRoundsByGameId(@Param("gameId") Long gameId);

    /**
     * Finde Runden die noch nicht von beiden Spielern beantwortet wurden
     */
    @Query("SELECT r FROM Round r WHERE r.game.id = :gameId AND " +
            "(r.answerPlayer1 IS NULL OR r.answerPlayer2 IS NULL)")
    List<Round> findIncompleteRoundsByGameId(@Param("gameId") Long gameId);

    /**
     * Berechne die Gesamtpunkte eines Spielers in einem Spiel
     */
    @Query("SELECT COALESCE(SUM(r.pointsPlayer1), 0) FROM Round r WHERE r.game.id = :gameId")
    int calculateTotalPointsPlayer1(@Param("gameId") Long gameId);

    @Query("SELECT COALESCE(SUM(r.pointsPlayer2), 0) FROM Round r WHERE r.game.id = :gameId")
    int calculateTotalPointsPlayer2(@Param("gameId") Long gameId);
}