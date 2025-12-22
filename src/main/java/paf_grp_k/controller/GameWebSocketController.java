package paf_grp_k.controller;

import paf_grp_k.dto.*;
import paf_grp_k.model.Game;
import paf_grp_k.model.Player;
import paf_grp_k.model.Question;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import paf_grp_k.service.GameService;
import paf_grp_k.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import paf_grp_k.util.CategoryUtil;

import java.security.Principal;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(GameWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final QuestionRepository questionRepository;
    private final LobbyService lobbyService;
    private final GameService gameService;

    /* -------------------------- JOIN LOBBY -------------------------- */
    @MessageMapping("/game.join")
    public void joinLobby(@Payload JoinLobbyRequest request, Principal principal) {
        log.info("🎮 GAME.JOIN | Principal: {}", principal != null ? principal.getName() : "NULL");

        try {
            Player player = playerRepository.findById(request.getPlayerId())
                    .orElseThrow(() -> new RuntimeException("Spieler nicht gefunden"));

            String category = CategoryUtil.normalize(request.getCategory());

            LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(player.getId(), category);

            sendWithPrincipalSupport(principal, player.getId(),
                    "/queue/lobby.status", lobbyStatus);

            broadcastLobbyUpdate(category, "PLAYER_JOINED");

            // 🔒 THREAD-SICHERES MATCHMAKING
            synchronized (this) {
                lobbyService.checkForMatch(category).ifPresent(match -> {

                    // 🧹 Spieler aus Lobby entfernen
                    lobbyService.leaveLobby(match.player1Id, match.category);
                    lobbyService.leaveLobby(match.player2Id, match.category);

                    Game game = gameService.createGame(
                            match.player1Id,
                            match.player2Id,
                            match.category
                    );

                    notifyPlayersAboutMatch(game, match.player1Id, match.player2Id);
                    startGameWithDelayAsync(game);

                    log.info("🎮 MATCH gestartet: {} vs {}", match.player1Id, match.player2Id);
                });
            }

        } catch (Exception e) {
            log.error("FEHLER in joinLobby", e);
            sendWithPrincipalSupport(principal, request.getPlayerId(),
                    "/queue/game.error", e.getMessage());
        }
    }

    /* -------------------------- LEAVE LOBBY -------------------------- */
    @MessageMapping("/game.leave")
    public void leaveLobby(@Payload JoinLobbyRequest request, Principal principal) {
        lobbyService.leaveLobby(request.getPlayerId(), request.getCategory());

        sendWithPrincipalSupport(principal, request.getPlayerId(),
                "/queue/lobby.status",
                new LobbyStatusDTO("LEFT", "Lobby verlassen"));

        broadcastLobbyUpdate(request.getCategory(), "PLAYER_LEFT");
    }

    /* -------------------------- ANSWER -------------------------- */
    @MessageMapping("/game.answer")
    public void submitAnswer(@Payload PlayerAnswerRequest request, Principal principal) {

        sendWithPrincipalSupport(principal, request.getPlayerId(),
                "/queue/game.answer.confirm", "Antwort erhalten");

        messagingTemplate.convertAndSend(
                "/topic/game." + request.getGameId(),
                Map.of(
                        "type", "ANSWER_SUBMITTED",
                        "playerId", request.getPlayerId(),
                        "roundNumber", request.getRoundNumber(),
                        "answer", request.getSelectedAnswer()
                )
        );
    }

    /* -------------------------- BROADCAST -------------------------- */
    private void broadcastLobbyUpdate(String category, String status) {
        LobbyService.LobbyInfo lobbyInfo = lobbyService.getLobbyInfo(category);

        for (Long playerId : lobbyInfo.playerIds) {
            LobbyUpdateMessage update = new LobbyUpdateMessage(
                    category,
                    lobbyInfo.playerCount,
                    getPositionInQueue(playerId, lobbyInfo.playerIds),
                    status
            );
            sendWithPrincipalSupport(null, playerId, "/queue/lobby.updates", update);
        }
    }

    private int getPositionInQueue(Long playerId, List<Long> playerIds) {
        return playerIds.indexOf(playerId) + 1;
    }

    /* -------------------------- MATCH NOTIFY -------------------------- */
    private void notifyPlayersAboutMatch(Game game, Long p1, Long p2) {
        Player player1 = playerRepository.findById(p1).orElseThrow();
        Player player2 = playerRepository.findById(p2).orElseThrow();

        messagingTemplate.convertAndSendToUser(
                p1.toString(), "/queue/game.match",
                new GameMatchMessage(game.getId(), convertToPlayerDTO(player2), game.getCategory())
        );

        messagingTemplate.convertAndSendToUser(
                p2.toString(), "/queue/game.match",
                new GameMatchMessage(game.getId(), convertToPlayerDTO(player1), game.getCategory())
        );
    }

    /* -------------------------- GAME FLOW -------------------------- */
    @Async
    public void startGameWithDelayAsync(Game game) {
        try {
            for (int i = 5; i > 0; i--) {
                messagingTemplate.convertAndSendToUser(
                        game.getPlayer1().getId().toString(),
                        "/queue/game.countdown", Map.of("seconds", i)
                );
                messagingTemplate.convertAndSendToUser(
                        game.getPlayer2().getId().toString(),
                        "/queue/game.countdown", Map.of("seconds", i)
                );
                Thread.sleep(1000);
            }
            gameService.startGame(game.getId());
            startNewRound(game, 1);
        } catch (Exception e) {
            log.error("FEHLER beim Spielstart", e);
        }
    }

    private void startNewRound(Game game, int roundNumber) {
        List<Question> questions =
                questionRepository.findRandomQuestionsByCategory(game.getCategory(), 1);

        if (questions.isEmpty()) {
            endGame(game, "Keine Fragen verfügbar");
            return;
        }

        messagingTemplate.convertAndSend(
                "/topic/game." + game.getId(),
                Map.of(
                        "type", "ROUND_START",
                        "roundNumber", roundNumber,
                        "question", questions.get(0),
                        "timeLimit", 30000
                )
        );
    }

    private void endGame(Game game, String message) {
        gameService.finishGame(game.getId());
        messagingTemplate.convertAndSend(
                "/topic/game." + game.getId(),
                Map.of("type", "GAME_END", "message", message)
        );
    }

    /* -------------------------- HELPER -------------------------- */
    private void sendWithPrincipalSupport(Principal principal, Long playerId,
                                          String destination, Object payload) {
        String user = principal != null ? principal.getName()
                : playerId != null ? playerId.toString() : null;

        if (user != null) {
            messagingTemplate.convertAndSendToUser(user, destination, payload);
        }
    }

    private PlayerDTO convertToPlayerDTO(Player player) {
        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setUsername(player.getUsername());
        dto.setProfileImageUrl(player.getProfileImageUrl());
        dto.setTotalGames(player.getTotalGames());
        dto.setGamesWon(player.getGamesWon());
        dto.setGamesLost(player.getGamesLost());
        dto.setHighscore(player.getHighscore());
        return dto;
    }
}
