package paf_grp_k.repository;

import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import paf_grp_k.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {

    // Methode 1: Mit Spieler-Objekten
    List<Game> findByPlayer1OrPlayer2(Player player1, Player player2);

    // Methode 2: Mit Spieler-IDs (NEU HINZUFÜGEN!)
    List<Game> findByPlayer1IdOrPlayer2Id(Long player1Id, Long player2Id);

    @Query("SELECT g FROM Game g WHERE (g.player1.id = :playerId OR g.player2.id = :playerId) AND g.status = :status")
    Optional<Game> findByPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") GameStatus status);

    List<Game> findByStatus(GameStatus status);

    List<Game> findByCategory(String category);
}