package paf_grp_k.controller;

import paf_grp_k.dto.*;
import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import paf_grp_k.model.Player;
import paf_grp_k.model.Question;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import paf_grp_k.service.GameService;
import paf_grp_k.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * WebSocket-Controller für das QuizDuell-Spiel.
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PlayerRepository playerRepository;
    private final GameRepository gameRepository;
    private final QuestionRepository questionRepository;
    private final LobbyService lobbyService;
    private final GameService gameService;

    @MessageMapping("/game.join")
    public void joinLobby(@Payload JoinLobbyRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        String category = request.getCategory() != null ? request.getCategory() : "ALL";

        // ACHTUNG: Jetzt LobbyStatusDTO statt LobbyStatus
        LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(request.getPlayerId(), category);

        messagingTemplate.convertAndSendToUser(
                player.getId().toString(),
                "/queue/lobby.status",
                lobbyStatus
        );

        checkForMatchAndStartGame(category);
    }

    @MessageMapping("/game.leave")
    public void leaveLobby(@Payload JoinLobbyRequest request) {
        lobbyService.leaveLobby(request.getPlayerId(), request.getCategory());

        // ACHTUNG: Jetzt LobbyStatusDTO
        LobbyStatusDTO status = new LobbyStatusDTO("LEFT", "Lobby verlassen");
        messagingTemplate.convertAndSendToUser(
                request.getPlayerId().toString(),
                "/queue/lobby.status",
                status
        );
    }

    @MessageMapping("/game.answer")
    public void submitAnswer(@Payload PlayerAnswerRequest request) {
        try {
            System.out.println("Spieler " + request.getPlayerId() + " antwortet: " + request.getSelectedAnswer());

            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/game.answer.confirm",
                    "Antwort erhalten"
            );

            Map<String, Object> answerMessage = new HashMap<>();
            answerMessage.put("type", "ANSWER_SUBMITTED");
            answerMessage.put("playerId", request.getPlayerId());
            answerMessage.put("roundNumber", request.getRoundNumber());
            answerMessage.put("answer", request.getSelectedAnswer());

            messagingTemplate.convertAndSend(
                    "/topic/game." + request.getGameId(),
                    answerMessage
            );

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/game.error",
                    "Fehler bei der Antwortverarbeitung: " + e.getMessage()
            );
        }
    }

    private void checkForMatchAndStartGame(String category) {
        Optional<LobbyService.MatchResult> matchOpt = lobbyService.checkForMatch(category);

        if (matchOpt.isPresent()) {
            LobbyService.MatchResult match = matchOpt.get();

            try {
                Game game = gameService.createGame(match.player1Id, match.player2Id, match.category);
                notifyPlayersAboutMatch(game, match.player1Id, match.player2Id);
                startGameWithDelay(game);

            } catch (Exception e) {
                System.err.println("Fehler beim Spielstart: " + e.getMessage());
                lobbyService.joinLobby(match.player1Id, category);
                lobbyService.joinLobby(match.player2Id, category);
            }
        }
    }

    private void notifyPlayersAboutMatch(Game game, Long player1Id, Long player2Id) {
        Player player1 = playerRepository.findById(player1Id).orElseThrow();
        Player player2 = playerRepository.findById(player2Id).orElseThrow();

        PlayerDTO opponent1 = convertToPlayerDTO(player2);
        PlayerDTO opponent2 = convertToPlayerDTO(player1);

        GameMatchMessage message1 = new GameMatchMessage(game.getId(), opponent1, game.getCategory());
        GameMatchMessage message2 = new GameMatchMessage(game.getId(), opponent2, game.getCategory());

        messagingTemplate.convertAndSendToUser(player1Id.toString(), "/queue/game.match", message1);
        messagingTemplate.convertAndSendToUser(player2Id.toString(), "/queue/game.match", message2);
    }

    private void startGameWithDelay(Game game) {
        new Thread(() -> {
            try {
                // Countdown
                for (int i = 5; i > 0; i--) {
                    Map<String, Object> countdownMessage = new HashMap<>();
                    countdownMessage.put("type", "COUNTDOWN");
                    countdownMessage.put("seconds", i);
                    countdownMessage.put("message", "Spiel startet in " + i + " Sekunden...");

                    messagingTemplate.convertAndSendToUser(
                            game.getPlayer1().getId().toString(),
                            "/queue/game.countdown",
                            countdownMessage
                    );
                    messagingTemplate.convertAndSendToUser(
                            game.getPlayer2().getId().toString(),
                            "/queue/game.countdown",
                            countdownMessage
                    );

                    Thread.sleep(1000);
                }

                // Spiel starten
                gameService.startGame(game.getId());

                // GameStartMessage senden
                GameStartMessage startMessage1 = new GameStartMessage(
                        game.getId(),
                        game.getPlayer2().getId(),
                        game.getPlayer2().getUsername()
                );
                GameStartMessage startMessage2 = new GameStartMessage(
                        game.getId(),
                        game.getPlayer1().getId(),
                        game.getPlayer1().getUsername()
                );

                messagingTemplate.convertAndSendToUser(
                        game.getPlayer1().getId().toString(),
                        "/queue/game.start",
                        startMessage1
                );
                messagingTemplate.convertAndSendToUser(
                        game.getPlayer2().getId().toString(),
                        "/queue/game.start",
                        startMessage2
                );

                // Erste Runde starten
                startNewRound(game, 1);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Countdown unterbrochen: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("Fehler beim Spielstart: " + e.getMessage());
            }
        }).start();
    }

    private void startNewRound(Game game, int roundNumber) {
        try {
            // Runde erstellen
            gameService.startNewRound(game.getId(), roundNumber);

            // Frage holen
            List<Question> questions = questionRepository.findRandomQuestionsByCategory(
                    game.getCategory(), 1
            );

            if (questions.isEmpty()) {
                questions = questionRepository.findRandomQuestions(1);
            }

            if (questions.isEmpty()) {
                endGame(game, "Spiel beendet - keine Fragen verfügbar");
                return;
            }

            Question question = questions.get(0);

            // Runden-Start senden
            Map<String, Object> roundMessage = new HashMap<>();
            roundMessage.put("type", "ROUND_START");
            roundMessage.put("roundNumber", roundNumber);
            roundMessage.put("question", question);
            roundMessage.put("timeLimit", 30000);
            roundMessage.put("gameId", game.getId());

            messagingTemplate.convertAndSend("/topic/game." + game.getId(), roundMessage);

        } catch (Exception e) {
            System.err.println("Fehler beim Rundenstart: " + e.getMessage());
            endGame(game, "Fehler beim Rundenstart: " + e.getMessage());
        }
    }

    private void endGame(Game game, String message) {
        try {
            gameService.finishGame(game.getId());

            Map<String, Object> endMessage = new HashMap<>();
            endMessage.put("type", "GAME_END");
            endMessage.put("message", message);
            endMessage.put("finalScorePlayer1", game.getScorePlayer1());
            endMessage.put("finalScorePlayer2", game.getScorePlayer2());
            endMessage.put("winnerId", game.getWinner() != null ? game.getWinner().getId() : null);

            messagingTemplate.convertAndSend("/topic/game." + game.getId(), endMessage);

        } catch (Exception e) {
            System.err.println("Fehler beim Spielende: " + e.getMessage());
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