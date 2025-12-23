package paf_grp_k.controller;

import paf_grp_k.dto.*;
import paf_grp_k.model.Game;
import paf_grp_k.model.Player;
import paf_grp_k.model.Round;
import paf_grp_k.service.GameService;
import paf_grp_k.service.LobbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;

import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final LobbyService lobbyService;
    private final GameService gameService;
    private final paf_grp_k.repository.PlayerRepository playerRepository;

    private final Map<Long, String> playerAnswers = new HashMap<>();
    private final Map<Long, Integer> roundScores = new HashMap<>();

    // ========== LOBBY & MATCHMAKING ==========

    @MessageMapping("/game.join")
    public void handleJoinLobby(@Payload JoinLobbyRequest request) {
        try {
            log.info("🎮 Spieler {} möchte Lobby {} betreten",
                    request.getPlayerId(), request.getCategory());

            // 1. Spieler der Lobby hinzufügen
            LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(
                    request.getPlayerId(),
                    request.getCategory()
            );

            // 2. Status an Spieler senden
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/lobby.status",
                    lobbyStatus
            );

            // 3. Lobby-Update an alle senden
            broadcastLobbyUpdate(request.getCategory());

            // 4. Matchmaking prüfen (nur wenn genug Spieler)
            checkForMatch(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.join: {}", e.getMessage());
            sendError(request.getPlayerId(), "Fehler beim Beitritt: " + e.getMessage());
        }
    }

    @MessageMapping("/game.leave")
    public void handleLeaveLobby(@Payload JoinLobbyRequest request) {
        try {
            log.info("🚪 Spieler {} verlässt Lobby {}",
                    request.getPlayerId(), request.getCategory());

            lobbyService.leaveLobby(request.getPlayerId(), request.getCategory());
            broadcastLobbyUpdate(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.leave: {}", e.getMessage());
        }
    }

    // ========== GAME PLAY ==========

    @MessageMapping("/game.answer")
    public void handleAnswer(@Payload AnswerRequest request) {
        try {
            log.info("📝 Spieler {} antwortet in Spiel {}: {}",
                    request.getPlayerId(), request.getGameId(), request.getAnswer());

            // 1. Antwort speichern
            String answerKey = request.getGameId() + "_" + request.getPlayerId() + "_" + request.getRoundNumber();
            playerAnswers.put(request.getGameId() + request.getPlayerId(), request.getAnswer());

            // 2. Spieler benachrichtigen
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/game.answer.confirmed",
                    Map.of(
                            "type", "ANSWER_CONFIRMED",
                            "gameId", request.getGameId(),
                            "roundNumber", request.getRoundNumber()
                    )
            );

            // 3. Gegner benachrichtigen, dass Antwort gegeben wurde
            Game game = gameService.getGameById(request.getGameId());
            Long opponentId = getOpponentId(game, request.getPlayerId());

            if (opponentId != null) {
                messagingTemplate.convertAndSendToUser(
                        opponentId.toString(),
                        "/queue/game.opponent.answered",
                        Map.of(
                                "type", "OPPONENT_ANSWERED",
                                "gameId", request.getGameId(),
                                "roundNumber", request.getRoundNumber()
                        )
                );
            }

            // 4. Wenn beide geantwortet haben, Punkte berechnen
            checkIfBothAnswered(game, request.getRoundNumber());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.answer: {}", e.getMessage());
        }
    }

    @MessageMapping("/game.nextRound")
    public void handleNextRound(@Payload NextRoundRequest request) {
        try {
            log.info("🔄 Nächste Runde für Spiel {}: Runde {}",
                    request.getGameId(), request.getRoundNumber());

            Game game = gameService.getGameById(request.getGameId());

            // Neue Runde starten
            Round round = gameService.startNewRound(request.getGameId(), request.getRoundNumber());

            // Frage an beide Spieler senden
            Map<String, Object> roundStartMsg = Map.of(
                    "type", "ROUND_START",
                    "gameId", request.getGameId(),
                    "roundNumber", request.getRoundNumber(),
                    "question", round.getQuestion(),
                    "timeLimit", 30000,
                    "totalRounds", 5
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + request.getGameId(),
                    roundStartMsg
            );

            // Reset Antworten für neue Runde
            playerAnswers.remove(request.getGameId() + game.getPlayer1().getId());
            playerAnswers.remove(request.getGameId() + game.getPlayer2().getId());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.nextRound: {}", e.getMessage());
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void checkForMatch(String category) {
        Optional<LobbyService.MatchResult> matchOpt =
                lobbyService.checkAndCreateMatch(category);

        if (matchOpt.isPresent()) {
            LobbyService.MatchResult match = matchOpt.get();
            log.info("✅ Match gefunden: {} vs {} (Kategorie: {})",
                    match.player1Id, match.player2Id, match.category);

            // Spiel erstellen
            Game game = gameService.createGame(
                    match.player1Id,
                    match.player2Id,
                    match.category
            );

            // Spieler benachrichtigen
            notifyMatchFound(game, match.player1Id, match.player2Id);

            // Countdown starten
            startGameCountdownAsync(game);
        }
    }

    private void notifyMatchFound(Game game, Long player1Id, Long player2Id) {
        log.info("🎯 SENDING MATCH NOTIFICATION: Game {} for players {} and {}",
                game.getId(), player1Id, player2Id);

        Player player1 = playerRepository.findById(player1Id).orElseThrow();
        Player player2 = playerRepository.findById(player2Id).orElseThrow();

        // Spieler 1 benachrichtigen
        GameMatchMessage matchMsg1 = new GameMatchMessage(
                game.getId(),
                convertToPlayerDTO(player2),
                game.getCategory()
        );

        // Spieler 2 benachrichtigen
        GameMatchMessage matchMsg2 = new GameMatchMessage(
                game.getId(),
                convertToPlayerDTO(player1),
                game.getCategory()
        );

        messagingTemplate.convertAndSendToUser(
                player1Id.toString(),
                "/queue/game.match",
                matchMsg1
        );

        messagingTemplate.convertAndSendToUser(
                player2Id.toString(),
                "/queue/game.match",
                matchMsg2
        );

        log.info("✅ Match notifications sent to /queue/game.match");    }

    @Async
    public void startGameCountdownAsync(Game game) {
        try {
            // 5-Sekunden Countdown
            for (int i = 5; i > 0; i--) {
                Map<String, Object> countdownMsg = Map.of(
                        "type", "COUNTDOWN",
                        "gameId", game.getId(),
                        "seconds", i,
                        "message", "Spiel startet in " + i + " Sekunden..."
                );

                messagingTemplate.convertAndSend(
                        "/topic/game/" + game.getId() + "/countdown",
                        countdownMsg
                );
                Thread.sleep(1000);
            }

            // Spiel starten
            gameService.startGame(game.getId());

            // Erste Runde starten
            Round firstRound = gameService.startNewRound(game.getId(), 1);

            Map<String, Object> startMsg = Map.of(
                    "type", "GAME_START",
                    "gameId", game.getId(),
                    "message", "Spiel gestartet!",
                    "player1", convertToPlayerDTO(game.getPlayer1()),
                    "player2", convertToPlayerDTO(game.getPlayer2()),
                    "category", game.getCategory(),
                    "round", Map.of(
                            "number", 1,
                            "question", firstRound.getQuestion(),
                            "timeLimit", 30000
                    ),
                    "totalRounds", 5
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId(),
                    startMsg
            );

            log.info("🎮 Spiel {} gestartet", game.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Countdown unterbrochen");
        } catch (Exception e) {
            log.error("❌ Fehler beim Spielstart: {}", e.getMessage());
        }
    }

    private void checkIfBothAnswered(Game game, int roundNumber) {
        Long player1Id = game.getPlayer1().getId();
        Long player2Id = game.getPlayer2().getId();

        String answer1 = playerAnswers.get(game.getId() + player1Id);
        String answer2 = playerAnswers.get(game.getId() + player2Id);

        if (answer1 != null && answer2 != null) {
            // Beide haben geantwortet - Punkte berechnen
            calculateRoundPoints(game, roundNumber, answer1, answer2);

            // Antworten für diese Runde löschen
            playerAnswers.remove(game.getId() + player1Id);
            playerAnswers.remove(game.getId() + player2Id);
        }
    }

    private void calculateRoundPoints(Game game, int roundNumber, String answer1, String answer2) {
        try {
            // Hole die aktuelle Runde
            Round round = gameService.getCurrentRound(game.getId(), roundNumber);
            String correctAnswer = round.getQuestion().getCorrectAnswer();

            // Punkte berechnen
            int points1 = correctAnswer.equals(answer1) ? 10 : 0;
            int points2 = correctAnswer.equals(answer2) ? 10 : 0;

            // Punkte speichern
            round.setPointsPlayer1(points1);
            round.setPointsPlayer2(points2);

            // Gesamtscore aktualisieren
            game.setScorePlayer1(game.getScorePlayer1() + points1);
            game.setScorePlayer2(game.getScorePlayer2() + points2);

            // Ergebnis an beide Spieler senden
            RoundResultDTO result = new RoundResultDTO(
                    game.getId(),
                    roundNumber,
                    correctAnswer,
                    points1,
                    points2,
                    getRoundResultMessage(points1, points2),
                    roundNumber >= 5 // Wenn Runde 5 erreicht ist, Spiel beenden
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId() + "/result",
                    Map.of(
                            "type", "ROUND_RESULT",
                            "result", result,
                            "scores", Map.of(
                                    "player1", game.getScorePlayer1(),
                                    "player2", game.getScorePlayer2()
                            )
                    )
            );

            log.info("📊 Runde {} Ergebnis: Spieler1={} Punkte, Spieler2={} Punkte",
                    roundNumber, points1, points2);

            // Wenn letzte Runde, Spiel beenden
            if (roundNumber >= 5) {
                finishGame(game);
            }

        } catch (Exception e) {
            log.error("❌ Fehler bei Punkteberechnung: {}", e.getMessage());
        }
    }

    private void finishGame(Game game) {
        try {
            // Spiel beenden
            gameService.finishGame(game.getId());

            Map<String, Object> endMsg = Map.of(
                    "type", "GAME_END",
                    "gameId", game.getId(),
                    "winner", game.getWinner() != null ? game.getWinner().getId() : null,
                    "finalScores", Map.of(
                            "player1", game.getScorePlayer1(),
                            "player2", game.getScorePlayer2()
                    ),
                    "message", getGameEndMessage(game)
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId() + "/end",
                    endMsg
            );

            log.info("🏁 Spiel {} beendet. Sieger: {}", game.getId(),
                    game.getWinner() != null ? game.getWinner().getUsername() : "Unentschieden");

        } catch (Exception e) {
            log.error("❌ Fehler beim Spielende: {}", e.getMessage());
        }
    }

    private String getRoundResultMessage(int points1, int points2) {
        if (points1 > points2) {
            return "Du hast diese Runde gewonnen!";
        } else if (points2 > points1) {
            return "Dein Gegner hat diese Runde gewonnen!";
        } else {
            return "Diese Runde ist unentschieden!";
        }
    }

    private String getGameEndMessage(Game game) {
        if (game.getWinner() == null) {
            return "Unentschieden! Beide Spieler waren gleich stark!";
        }

        String winnerName = game.getWinner().getUsername();
        int winnerScore = game.getWinner().getId().equals(game.getPlayer1().getId())
                ? game.getScorePlayer1() : game.getScorePlayer2();
        int loserScore = game.getWinner().getId().equals(game.getPlayer1().getId())
                ? game.getScorePlayer2() : game.getScorePlayer1();

        return String.format("%s hat mit %d:%d gewonnen!",
                winnerName, winnerScore, loserScore);
    }

    private Long getOpponentId(Game game, Long playerId) {
        if (game.getPlayer1().getId().equals(playerId)) {
            return game.getPlayer2().getId();
        } else if (game.getPlayer2().getId().equals(playerId)) {
            return game.getPlayer1().getId();
        }
        return null;
    }

    private void broadcastLobbyUpdate(String category) {
        LobbyService.LobbyInfo lobbyInfo = lobbyService.getLobbyInfo(category);

        Map<String, Object> update = Map.of(
                "type", "LOBBY_UPDATE",
                "category", category,
                "playerCount", lobbyInfo.playerCount,
                "playerIds", lobbyInfo.playerIds,
                "timestamp", System.currentTimeMillis()
        );

        messagingTemplate.convertAndSend(
                "/topic/lobby/" + category,
                update
        );
    }

    private void sendError(Long playerId, String message) {
        Map<String, String> error = Map.of(
                "type", "ERROR",
                "message", message
        );

        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/errors",
                error
        );
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