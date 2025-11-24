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

@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @GetMapping
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    @GetMapping("/{id}")
    public Game getGameById(@PathVariable Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + id));
    }

    @GetMapping("/player/{playerId}")
    public List<Game> getGamesByPlayer(@PathVariable Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));
        return gameRepository.findByPlayer1OrPlayer2(player, player);
    }

    @GetMapping("/waiting")
    public List<Game> getWaitingGames() {
        return gameRepository.findByStatus(GameStatus.WAITING);
    }

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

    @PostMapping("/{id}/start")
    public Game startGame(@PathVariable Long id) {
        Game game = gameRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Game not found"));
        game.setStatus(GameStatus.IN_PROGRESS);
        return gameRepository.save(game);
    }

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