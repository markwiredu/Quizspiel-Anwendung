package paf_grp_k.repository;

import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import paf_grp_k.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository-Schnittstelle für den Zugriff auf {@link Game}-Entitäten.
 *
 * <p>Diese Schnittstelle erweitert {@link JpaRepository} und stellt damit
 * grundlegende CRUD-Methoden für Spiele bereit. Zusätzlich werden
 * vordefinierte Query-Methoden angeboten, um Spiele eines Spielers
 * sowie Spiele nach ihrem Status abzufragen.</p>
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Findet alle Spiele, an denen der angegebene Spieler als Spieler 1
     * oder als Spieler 2 beteiligt ist.
     *
     * @param player1 der Spieler, der als Player1 verglichen wird
     * @param player2 der Spieler, der als Player2 verglichen wird
     * @return eine Liste aller Spiele, in denen der Spieler Teil des Spiels ist
     */
    List<Game> findByPlayer1OrPlayer2(Player player1, Player player2);

    /**
     * Findet alle Spiele, die den angegebenen Status haben.
     *
     * @param status der Spielstatus (z. B. WAITING, IN_PROGRESS, FINISHED)
     * @return eine Liste aller Spiele mit diesem Status
     */
    List<Game> findByStatus(GameStatus status);
}
