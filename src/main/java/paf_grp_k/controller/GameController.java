package paf_grp_k.controller;

import paf_grp_k.dto.CreateGameRequest;
import paf_grp_k.dto.FinishGameRequest;
import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import paf_grp_k.model.Player;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST-Controller zur Verwaltung von Spielen.
 * <p>
 * Stellt Endpunkte zum Erstellen, Abrufen, Starten und Beenden von Spielen bereit.
 * Alle Endpunkte sind unter dem Pfad {@code /api/games} erreichbar.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    /**
     * Repository für den Zugriff auf Spiel-Daten.
     */
    @Autowired
    private GameRepository gameRepository;

    /**
     * Repository für den Zugriff auf Spieler-Daten.
     */
    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Liefert alle gespeicherten Spiele.
     *
     * @return Liste aller Spiele
     */
    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    /**
     * Liefert ein einzelnes Spiel anhand seiner ID.
     *
     * @param id ID des gesuchten Spiels
     * @return das gefundene Spiel
     * @throws RuntimeException falls kein Spiel mit dieser ID existiert
     */
    @GetMapping("/{id}")
    public Game getGameById(@PathVariable Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + id));
    }

    /**
     * Liefert alle Spiele, an denen ein bestimmter Spieler beteiligt ist.
     *
     * @param playerId ID des Spielers
     * @return Liste der Spiele des Spielers
     * @throws RuntimeException falls der Spieler nicht existiert
     */
    @GetMapping("/player/{playerId}")
    public List<Game> getGamesByPlayer(@PathVariable Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        return gameRepository.findByPlayer1OrPlayer2(player, player);
    }

    /**
     * Liefert alle Spiele mit dem Status {@link GameStatus#WAITING}.
     *
     * @return Liste wartender Spiele
     */
    @GetMapping("/waiting")
    public List<Game> getWaitingGames() {
        return gameRepository.findByStatus(GameStatus.WAITING);
    }

    /**
     * Erstellt ein neues Spiel mit zwei Spielern.
     * <p>
     * Das Spiel wird mit dem Status {@link GameStatus#WAITING} angelegt
     * und die Startzeit wird auf den aktuellen Zeitpunkt gesetzt.
     *
     * @param request Request-Objekt mit den Spieler-IDs
     * @return das gespeicherte Spiel
     * @throws RuntimeException falls einer der Spieler nicht existiert
     */
    @PostMapping
    public Game createGame(@RequestBody CreateGameRequest request) {
        Player player1 = playerRepository.findById(request.getPlayer1Id())
                .orElseThrow(() -> new RuntimeException("Player 1 not found"));
        Player player2 = playerRepository.findById(request.getPlayer2Id())
                .orElseThrow(() -> new RuntimeException("Player 2 not found"));

        Game game = new Game();
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.WAITING);
        game.setStartTime(LocalDateTime.now());

        return gameRepository.save(game);
    }

    /**
     * Startet ein Spiel und setzt dessen Status auf {@link GameStatus#IN_PROGRESS}.
     *
     * @param id ID des Spiels
     * @return das aktualisierte Spiel
     * @throws RuntimeException falls das Spiel nicht existiert
     */
    @PostMapping("/{id}/start")
    public Game startGame(@PathVariable Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        game.setStatus(GameStatus.IN_PROGRESS);
        return gameRepository.save(game);
    }

    /**
     * Beendet ein Spiel und speichert das Ergebnis.
     * <p>
     * Setzt den Status auf {@link GameStatus#FINISHED}, speichert Endzeit,
     * Punktestände und optional den Gewinner.
     *
     * @param id      ID des Spiels
     * @param request Request-Objekt mit Spielergebnissen
     * @return das aktualisierte Spiel
     * @throws RuntimeException falls Spiel oder Gewinner nicht existieren
     */
    @PostMapping("/{id}/finish")
    public Game finishGame(@PathVariable Long id, @RequestBody FinishGameRequest request) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        game.setStatus(GameStatus.FINISHED);
        game.setEndTime(LocalDateTime.now());
        game.setScorePlayer1(request.getScorePlayer1());
        game.setScorePlayer2(request.getScorePlayer2());

        if (request.getWinnerId() != null) {
            Player winner = playerRepository.findById(request.getWinnerId())
                    .orElseThrow(() -> new RuntimeException("Winner not found"));
            game.setWinner(winner);
        }

        return gameRepository.save(game);
    }
}
