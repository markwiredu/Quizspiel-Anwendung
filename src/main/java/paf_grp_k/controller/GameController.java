package paf_grp_k.controller;

import paf_grp_k.model.Game;
import paf_grp_k.model.Round;
import paf_grp_k.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Controller zur Abfrage spielbezogener Daten.
 *
 * <p>Diese Klasse stellt Endpunkte zur Verfügung, um Spiele, Runden
 * und spielerbezogene Spielinformationen abzurufen. Die eigentliche
 * Geschäftslogik ist im {@link GameService} gekapselt.</p>
 *
 * <p>Der Controller dient ausschließlich als Schnittstelle zwischen
 * HTTP-Anfragen und der Service-Schicht.</p>
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    /**
     * Service zur Verarbeitung von Spiel- und Rundendaten.
     */
    private final GameService gameService;

    /**
     * Liefert alle Spiele, an denen ein bestimmter Spieler beteiligt ist.
     *
     * <p>Der Endpunkt gibt sowohl aktive als auch abgeschlossene Spiele zurück,
     * sofern diese dem angegebenen Spieler zugeordnet sind.</p>
     *
     * @param playerId eindeutige ID des Spielers
     * @return Liste aller Spiele des Spielers
     */
    @GetMapping("/player/{playerId}")
    public List<Game> getGamesByPlayer(@PathVariable Long playerId) {
        return gameService.getGamesByPlayerId(playerId);
    }

    /**
     * Liefert ein einzelnes Spiel anhand seiner ID.
     *
     * @param gameId eindeutige ID des Spiels
     * @return das angeforderte {@link Game}
     */
    @GetMapping("/{gameId}")
    public Game getGameById(@PathVariable Long gameId) {
        return gameService.getGameById(gameId);
    }

    /**
     * Liefert das aktuell aktive Spiel eines Spielers.
     *
     * <p>Ein aktives Spiel ist ein Spiel, das noch nicht abgeschlossen ist.
     * Existiert kein aktives Spiel, kann {@code null} zurückgegeben werden.</p>
     *
     * @param playerId eindeutige ID des Spielers
     * @return aktives {@link Game} oder {@code null}, falls keines existiert
     */
    @GetMapping("/active/{playerId}")
    public Game getActiveGame(@PathVariable Long playerId) {
        return gameService.getActiveGameByPlayer(playerId);
    }

    /**
     * Liefert alle Runden, die zu einem bestimmten Spiel gehören.
     *
     * <p>Die Reihenfolge der Runden entspricht der vom Service definierten
     * Sortierung (z. B. chronologisch).</p>
     *
     * @param gameId eindeutige ID des Spiels
     * @return Liste der {@link Round}-Objekte des Spiels
     */
    @GetMapping("/{gameId}/rounds")
    public List<Round> getGameRounds(@PathVariable Long gameId) {
        return gameService.getGameRounds(gameId);
    }
}
