package paf_grp_k.controller;

import paf_grp_k.model.Game;
import paf_grp_k.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;  // Service korrekt injizieren

    /**
     * Liefert alle Spiele, an denen ein bestimmter Spieler beteiligt ist.
     *
     * @param playerId ID des Spielers
     * @return Liste der Spiele des Spielers
     */
    @GetMapping("/player/{playerId}")
    public List<Game> getGamesByPlayer(@PathVariable Long playerId) {
        return gameService.getGamesByPlayerId(playerId);
    }

    /**
     * Liefert ein Spiel anhand seiner ID.
     *
     * @param gameId ID des Spiels
     * @return das Spiel
     */
    @GetMapping("/{gameId}")
    public Game getGameById(@PathVariable Long gameId) {
        return gameService.getGameById(gameId);
    }

    /**
     * Liefert das aktive Spiel eines Spielers (falls vorhanden).
     *
     * @param playerId ID des Spielers
     * @return aktives Spiel oder null
     */
    @GetMapping("/active/{playerId}")
    public Game getActiveGame(@PathVariable Long playerId) {
        return gameService.getActiveGameByPlayer(playerId);
    }

    /**
     * Liefert alle Runden eines Spiels.
     *
     * @param gameId ID des Spiels
     * @return Liste der Runden
     */
    @GetMapping("/{gameId}/rounds")
    public List<paf_grp_k.model.Round> getGameRounds(@PathVariable Long gameId) {
        return gameService.getGameRounds(gameId);
    }
}