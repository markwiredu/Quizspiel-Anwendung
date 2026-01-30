package paf_grp_k.repository;

import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository-Interface für den Zugriff auf {@link Game}-Entitäten.
 *
 * <p>Dieses Repository kapselt alle datenbankbezogenen Abfragen
 * rund um Spiele und wird typischerweise von Services wie
 * {@code GameService} verwendet.</p>
 *
 * <p>Durch die Erweiterung von {@link JpaRepository} stehen
 * Standard-CRUD-Operationen automatisch zur Verfügung.</p>
 */
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Liefert alle Spiele, an denen ein bestimmter Spieler beteiligt ist.
     *
     * <p>Ein Spiel wird berücksichtigt, wenn der Spieler entweder
     * als {@code player1} oder {@code player2} eingetragen ist.</p>
     *
     * @param player1Id ID des Spielers als Spieler 1
     * @param player2Id ID des Spielers als Spieler 2
     * @return Liste aller zugehörigen Spiele
     */
    List<Game> findByPlayer1IdOrPlayer2Id(Long player1Id, Long player2Id);

    /**
     * Liefert ein Spiel eines Spielers mit einem bestimmten Status.
     *
     * <p>Wird typischerweise verwendet, um z. B. das aktuell aktive
     * Spiel eines Spielers zu ermitteln.</p>
     *
     * <p>Die Abfrage prüft sowohl {@code player1} als auch {@code player2}.</p>
     *
     * @param playerId ID des Spielers
     * @param status gesuchter {@link GameStatus}
     * @return Optional mit dem gefundenen Spiel oder leer, falls keines existiert
     */
    @Query("""
           SELECT g
           FROM Game g
           WHERE (g.player1.id = :playerId OR g.player2.id = :playerId)
             AND g.status = :status
           """)
    Optional<Game> findByPlayerIdAndStatus(
            @Param("playerId") Long playerId,
            @Param("status") GameStatus status
    );
}
