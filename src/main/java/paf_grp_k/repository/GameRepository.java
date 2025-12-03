package paf_grp_k.repository;

import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import paf_grp_k.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository für Game-Entities.
 * Stellt vordefinierte und abgeleitete Query-Methoden bereit,
 * um Spiele anhand verschiedener Kriterien zu finden.
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Findet alle Spiele, in denen ein bestimmter Spieler beteiligt ist –
     * entweder als Player1 oder als Player2.
     *
     * @param player1 Spieler als Player 1
     * @param player2 Spieler als Player 2
     * @return Liste aller Spiele, an denen der Spieler beteiligt ist
     */
    List<Game> findByPlayer1OrPlayer2(Player player1, Player player2);

    /**
     * Findet alle Spiele mit einem bestimmten Status
     * (z. B. WAITING, IN_PROGRESS oder FINISHED).
     *
     * @param status Der zu filternde Spielstatus
     * @return Liste aller Spiele mit diesem Status
     */
    List<Game> findByStatus(GameStatus status);

    List<Game> findByPlayer1IdOrPlayer2Id(Long player1Id, Long player2Id);


    /**
     * Finde aktive Spiele eines Spielers (nicht beendet)
     */
    @Query("SELECT g FROM Game g WHERE (g.player1.id = :playerId OR g.player2.id = :playerId) " +
            "AND g.status != 'FINISHED'")
    List<Game> findActiveGamesByPlayerId(@Param("playerId") Long playerId);



    /**
     * Findet Spiele eines bestimmten Spielers mit einem bestimmten Status.
     * <p>
     * Achtung: Die Methode erzeugt folgende WHERE-Klausel:
     * <br>
     * <code>(player1 = ? AND status = ?) OR (player2 = ? AND status = ?)</code>
     * <br>
     * Das bedeutet, dass {@code status1} und {@code status2} getrennt wirken.
     * In der Praxis sollten beide Statuswerte identisch übergeben werden.
     * </p>
     *
     * @param player1 Spieler als Player 1
     * @param status1 Status für Spiele, bei denen der Spieler Player 1 ist
     * @param player2 Spieler als Player 2
     * @param status2 Status für Spiele, bei denen der Spieler Player 2 ist
     * @return Liste aller passenden Spiele
     */
    List<Game> findByPlayer1AndStatusOrPlayer2AndStatus(Player player1, GameStatus status1,
                                                        Player player2, GameStatus status2);

    /**
     * Finde Spiel wo beide Spieler matched sind
     */
    Optional<Game> findByPlayer1IdAndPlayer2IdAndStatus(Long player1Id, Long player2Id, GameStatus status);

    /**
     * Zähle Siege eines Spielers
     */
    @Query("SELECT COUNT(g) FROM Game g WHERE g.winner.id = :playerId")
    int countWinsByPlayerId(@Param("playerId") Long playerId);

    /**
     * Finde das letzte Spiel eines Spielers
     */
    @Query("SELECT g FROM Game g WHERE (g.player1.id = :playerId OR g.player2.id = :playerId) " +
            "ORDER BY g.startTime DESC LIMIT 1")
    Optional<Game> findLatestGameByPlayerId(@Param("playerId") Long playerId);
}
