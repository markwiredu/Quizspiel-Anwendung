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
import java.util.concurrent.*;

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
    
    // Timer-Verwaltung für Spielrunden
    private final Map<String, ScheduledFuture<?>> roundTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(10);
    
    // Konstante für Zeitlimit pro Runde (30 Sekunden)
    private static final int ROUND_TIME_LIMIT_SECONDS = 30;

    // ========== LOBBY & MATCHMAKING ==========

    @MessageMapping("/game.join")
    public void handleJoinLobby(@Payload JoinLobbyRequest request) {
        try {
            log.info("🎮 SPIELER {} MÖCHTE LOBBY '{}' BEITRETEN",
                    request.getPlayerId(), request.getCategory());

            // 1. Lobby-Update an alle senden
            broadcastLobbyUpdate(request.getCategory());

            // 2. Spieler der Lobby hinzufügen
            LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(
                    request.getPlayerId(),
                    request.getCategory()
            );

            // 3. Status an Spieler senden
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/lobby.status",
                    lobbyStatus
            );

            log.info("📤 Lobby-Status an Spieler {} gesendet: {}",
                    request.getPlayerId(), lobbyStatus.getStatus());

            // 4. Lobby-Update an alle senden
            broadcastLobbyUpdate(request.getCategory());

            // 5. Matchmaking prüfen
            checkForMatch(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.join: {}", e.getMessage());
            sendError(request.getPlayerId(), "Fehler beim Beitritt: " + e.getMessage());
        }
    }

    @MessageMapping("/game.leave")
    public void handleLeaveLobby(@Payload JoinLobbyRequest request) {
        try {
            log.info("🚪 SPIELER {} VERLÄSST LOBBY {}",
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

            if (request.getGameId() == null) {
                log.error("❌ Game-ID ist null in AnswerRequest!");
                sendError(request.getPlayerId(), "Game-ID fehlt in der Antwort");
                return;
            }
            // 1. Antwort speichern
            playerAnswers.put(request.getGameId() + request.getPlayerId(), request.getAnswer());


            // 2. Spieler benachrichtigen (nur Bestätigung, noch kein Ergebnis)
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/game.answer.confirmed",
                    Map.of(
                            "type", "ANSWER_CONFIRMED",
                            "gameId", request.getGameId(),
                            "roundNumber", request.getRoundNumber(),
                            "answer", request.getAnswer(),
                            "message", "📝 Deine Antwort wurde gesendet: \"" + request.getAnswer() + "\""
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

            // Alten Timer stoppen falls vorhanden
            stopRoundTimer(request.getGameId(), request.getRoundNumber() - 1);

            // Neue Runde starten
            Round round = gameService.startNewRound(request.getGameId(), request.getRoundNumber());

            // Frage an beide Spieler senden
            Map<String, Object> roundStartMsg = Map.of(
                    "type", "ROUND_START",
                    "gameId", request.getGameId(),
                    "roundNumber", request.getRoundNumber(),
                    "question", toPublicDto(round.getQuestion()),
                    "timeLimit", ROUND_TIME_LIMIT_SECONDS * 1000,
                    "totalRounds", 5
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + request.getGameId(),
                    roundStartMsg
            );

            // Reset Antworten für neue Runde
            playerAnswers.remove(request.getGameId() + game.getPlayer1().getId());
            playerAnswers.remove(request.getGameId() + game.getPlayer2().getId());

            // Timer für diese Runde starten (sendet TIMER_START automatisch)
            startRoundTimer(request.getGameId(), request.getRoundNumber(), game);

        } catch (Exception e) {
            log.error("❌ Fehler bei game.nextRound: {}", e.getMessage());
        }
    }

    // Füge diese Methode zur GameWebSocketController.java hinzu:
    @MessageMapping("/game.debug.sendMatch")
    public void debugSendMatch(@Payload Map<String, Object> request) {
        Long playerId = Long.valueOf(request.get("playerId").toString());
        String category = request.get("category").toString();

        log.info("🎯 DEBUG: Manuelles Match an Spieler {} senden", playerId);

        Player opponent = playerRepository.findById(999L).orElseGet(() -> {
            Player debugPlayer = new Player();
            debugPlayer.setId(999L);
            debugPlayer.setUsername("DEBUG_GEGNER");
            return debugPlayer;
        });

        GameMatchMessage testMsg = new GameMatchMessage(
                9999L,
                convertToPlayerDTO(opponent),
                category
        );

        // 1. Versuche user-queue
        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/game.match",
                testMsg
        );

        // 2. Sende an player-specific topic
        messagingTemplate.convertAndSend(
                "/topic/game/match/player/" + playerId,
                testMsg
        );

        // 3. Sende an debug topic
        messagingTemplate.convertAndSend(
                "/topic/debug/match",
                Map.of(
                        "type", "DEBUG_MATCH_SENT",
                        "playerId", playerId,
                        "message", "Test-Match gesendet",
                        "timestamp", System.currentTimeMillis()
                )
        );

        log.info("✅ Debug-Match an Spieler {} gesendet", playerId);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void checkForMatch(String category) {
        log.info("🔍 PRÜFE MATCH FÜR KATEGORIE: {}", category);

        Optional<LobbyService.MatchResult> matchOpt = lobbyService.checkAndCreateMatch(category);

        if (matchOpt.isPresent()) {
            LobbyService.MatchResult match = matchOpt.get();
            log.info("✅ MATCH GEFUNDEN: {} vs {} (Kategorie: {})",
                    match.player1Id, match.player2Id, match.category);

            try {
                // Spiel erstellen
                Game game = gameService.createGame(
                        match.player1Id,
                        match.player2Id,
                        match.category
                );

                log.info("🎮 SPIEL ERSTELLT: ID {}", game.getId());

                // Spieler aus Lobby entfernen
                lobbyService.removePlayersAfterMatch(
                        match.player1Id,
                        match.player2Id,
                        match.category
                );

                // Spieler benachrichtigen
                notifyMatchFound(game, match.player1Id, match.player2Id);

                // Countdown starten
                startGameCountdownAsync(game);

                // Lobby-Update broadcasten
                broadcastLobbyUpdate(category);

            } catch (Exception e) {
                log.error("❌ FEHLER BEIM ERSTELLEN DES SPIELS: {}", e.getMessage(), e);

                // Matchmaking zurücksetzen
                lobbyService.resetMatchmaking(
                        match.player1Id,
                        match.player2Id,
                        match.category
                );

                // Fehler an Spieler senden
                sendError(match.player1Id, "Fehler beim Spielstart: " + e.getMessage());
                sendError(match.player2Id, "Fehler beim Spielstart: " + e.getMessage());

                // Lobby-Update broadcasten
                broadcastLobbyUpdate(category);
            }
        } else {
            log.debug("⏳ KEIN MATCH IN LOBBY {} GEFUNDEN", category);
        }
    }


    private void notifyMatchFound(Game game, Long player1Id, Long player2Id) {
        log.info("🎯 SENDE MATCH-NOTIFICATION: Spiel {} für Spieler {} und {}",
                game.getId(), player1Id, player2Id);

        Player player1 = playerRepository.findById(player1Id).orElseThrow(() ->
                new RuntimeException("Spieler " + player1Id + " nicht gefunden"));
        Player player2 = playerRepository.findById(player2Id).orElseThrow(() ->
                new RuntimeException("Spieler " + player2Id + " nicht gefunden"));

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

        log.info("📨 Sende an Spieler {}: {}", player1Id, matchMsg1);
        log.info("📨 Sende an Spieler {}: {}", player2Id, matchMsg2);

        // WICHTIG: Verwende öffentliche Topics statt user-queue
        // 1. Spieler-spezifisches Topic (funktioniert immer)
        messagingTemplate.convertAndSend(
                "/topic/game/match/player/" + player1Id,
                matchMsg1
        );

        messagingTemplate.convertAndSend(
                "/topic/game/match/player/" + player2Id,
                matchMsg2
        );

        log.info("✅ Match notifications gesendet");

        // DEBUG: Sende zusätzlich an ein Topic für Debugging
        messagingTemplate.convertAndSend(
                "/topic/debug/match",
                Map.of(
                        "type", "DEBUG_MATCH",
                        "gameId", game.getId(),
                        "player1Id", player1Id,
                        "player2Id", player2Id,
                        "timestamp", System.currentTimeMillis()
                )
        );
    }


    @Async
    public void startGameCountdownAsync(Game game) {
        try {
            log.info("⏱️ STARTE COUNTDOWN FÜR SPIEL {}", game.getId());

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

            log.info("🚀 SPIEL {} STARTET JETZT", game.getId());

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
                            "question", toPublicDto(firstRound.getQuestion()),
                            "timeLimit", ROUND_TIME_LIMIT_SECONDS * 1000
                    ),
                    "totalRounds", 5
            );

            messagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId(),
                    startMsg
            );

            // Timer für erste Runde starten (sendet TIMER_START automatisch)
            startRoundTimer(game.getId(), 1, game);

            log.info("🎮 SPIEL {} GESTARTET mit Spieler {} vs {}",
                    game.getId(), game.getPlayer1().getId(), game.getPlayer2().getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Countdown unterbrochen");
        } catch (Exception e) {
            log.error("❌ Fehler beim Spielstart: {}", e.getMessage(), e);
        }
    }

    private void checkIfBothAnswered(Game game, int roundNumber) {
        Long player1Id = game.getPlayer1().getId();
        Long player2Id = game.getPlayer2().getId();

        String answer1 = playerAnswers.get(game.getId() + player1Id);
        String answer2 = playerAnswers.get(game.getId() + player2Id);

        if (answer1 != null && answer2 != null) {
            log.info("✅ BEIDE SPIELER HABEN GEANTWORTET für Runde {}", roundNumber);

            // Beide haben geantwortet - Punkte berechnen
            calculateRoundPoints(game, roundNumber, answer1, answer2);

            // Antworten für diese Runde löschen
            playerAnswers.remove(game.getId() + player1Id);
            playerAnswers.remove(game.getId() + player2Id);
        } else {
            log.debug("⏳ Warte auf Antworten: Spieler1={}, Spieler2={}",
                    answer1 != null ? "✅" : "❌",
                    answer2 != null ? "✅" : "❌");
        }
    }

    private void calculateRoundPoints(Game game, int roundNumber, String answer1, String answer2) {
        try {
            // Timer für diese Runde stoppen
            stopRoundTimer(game.getId(), roundNumber);

            // Hole die aktuelle Runde
            Round round = gameService.getCurrentRound(game.getId(), roundNumber);
            String correctAnswer = round.getQuestion().getCorrectAnswer();

            // WICHTIG: Hole die vollständige Frage
            paf_grp_k.model.Question question = round.getQuestion();

            Long player1Id = game.getPlayer1().getId();
            Long player2Id = game.getPlayer2().getId();

            // WICHTIG: Konvertiere Buchstaben zu vollständigen Antworten
            String fullAnswer1 = mapLetterToAnswer(answer1, question);
            String fullAnswer2 = mapLetterToAnswer(answer2, question);

            // DEBUG-Logging
            log.info("🔍 VERGLEICH RUNDE {}:", roundNumber);
            log.info("  Spieler 1: '{}' -> '{}'", answer1, fullAnswer1);
            log.info("  Spieler 2: '{}' -> '{}'", answer2, fullAnswer2);
            log.info("  Korrekte Antwort: '{}'", correctAnswer);
            log.info("  Optionen: A='{}', B='{}', C='{}', D='{}'",
                    question.getOptionA(), question.getOptionB(),
                    question.getOptionC(), question.getOptionD());

            // Jetzt korrekt vergleichen (vollständige Antworten)
            boolean isCorrect1 = correctAnswer.equalsIgnoreCase(fullAnswer1);
            boolean isCorrect2 = correctAnswer.equalsIgnoreCase(fullAnswer2);
            int points1 = isCorrect1 ? 10 : 0;
            int points2 = isCorrect2 ? 10 : 0;

            // Punkte speichern
            round.setPointsPlayer1(points1);
            round.setPointsPlayer2(points2);

            // Gesamtscore aktualisieren
            game.setScorePlayer1(game.getScorePlayer1() + points1);
            game.setScorePlayer2(game.getScorePlayer2() + points2);

            // Individuelle Benachrichtigungen für jeden Spieler
            // Spieler 1: Seine eigene Antwort
            sendAnswerFeedback(player1Id, game.getId(), roundNumber,
                    isCorrect1, fullAnswer1, correctAnswer, points1,
                    "Du hast " + (isCorrect1 ? "richtig" : "falsch") + " geantwortet!");

            // Spieler 1: Gegner-Information
            sendOpponentAnswerFeedback(player1Id, game.getId(), roundNumber,
                    isCorrect2, points2,
                    "Dein Gegner hat " + (isCorrect2 ? "richtig" : "falsch") + " geantwortet.");

            // Spieler 2: Seine eigene Antwort
            sendAnswerFeedback(player2Id, game.getId(), roundNumber,
                    isCorrect2, fullAnswer2, correctAnswer, points2,
                    "Du hast " + (isCorrect2 ? "richtig" : "falsch") + " geantwortet!");

            // Spieler 2: Gegner-Information
            sendOpponentAnswerFeedback(player2Id, game.getId(), roundNumber,
                    isCorrect1, points1,
                    "Dein Gegner hat " + (isCorrect1 ? "richtig" : "falsch") + " geantwortet.");

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

            log.info("📊 RUNDE {} ERGEBNIS: Spieler1={} Punkte ({}), Spieler2={} Punkte ({})",
                    roundNumber, points1, isCorrect1 ? "richtig" : "falsch",
                    points2, isCorrect2 ? "richtig" : "falsch");

            // Wenn letzte Runde, Spiel beenden
            if (roundNumber >= 5) {
                finishGame(game);
            }

        } catch (Exception e) {
            log.error("❌ Fehler bei Punkteberechnung: {}", e.getMessage(), e);
        }
    }

    /**
     * Wandelt Buchstaben (A, B, C, D) in vollständige Antworten um
     */
    private String mapLetterToAnswer(String letter, paf_grp_k.model.Question question) {
        if (letter == null || letter.equals("TIMEOUT") || letter.equals("X")) {
            return "TIMEOUT";
        }

        switch(letter.toUpperCase()) {
            case "A": return question.getOptionA();
            case "B": return question.getOptionB();
            case "C": return question.getOptionC();
            case "D": return question.getOptionD();
            default: return letter; // Falls schon vollständige Antwort
        }
    }

    private void finishGame(Game game) {
        try {
            log.info("🏁 BEENDE SPIEL {}", game.getId());

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

            log.info("🏁 SPIEL {} BEENDET. Sieger: {}", game.getId(),
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
        try {
            LobbyService.LobbyInfo lobbyInfo = lobbyService.getLobbyInfo(category);

            Map<String, Object> update = Map.of(
                    "type", "LOBBY_UPDATE",
                    "category", category,
                    "playerCount", lobbyInfo.playerCount,
                    "playerIds", lobbyInfo.playerIds,
                    "timestamp", System.currentTimeMillis()
            );

            log.debug("📡 BROADCAST LOBBY UPDATE für {}: {} Spieler",
                    category, lobbyInfo.playerCount);

            messagingTemplate.convertAndSend(
                    "/topic/lobby/" + category,
                    update
            );
        } catch (Exception e) {
            log.error("❌ Fehler beim Broadcast Lobby Update: {}", e.getMessage());
        }
    }

    private void sendError(Long playerId, String message) {
        Map<String, Object> error = Map.of(
                "type", "ERROR",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/errors",
                error
        );

        log.warn("⚠️ Fehler an Spieler {} gesendet: {}", playerId, message);
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

    private QuestionPublicDTO toPublicDto(paf_grp_k.model.Question q) {
        return new QuestionPublicDTO(
                q.getId(),
                q.getCategory(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD()
        );
    }

    // ========== TIMER-FUNKTIONALITÄT ==========

    /**
     * Startet einen Timer für eine Spielrunde.
     * Nach Ablauf der Zeit werden automatisch fehlende Antworten als falsch gewertet.
     */
    private void startRoundTimer(Long gameId, int roundNumber, Game game) {
        String timerKey = gameId + "_" + roundNumber;
        
        // Alten Timer stoppen falls vorhanden
        stopRoundTimer(gameId, roundNumber);

        log.info("⏱️ Starte Timer für Spiel {} Runde {} ({} Sekunden)", 
                gameId, roundNumber, ROUND_TIME_LIMIT_SECONDS);

        // Timer-Start-Benachrichtigung senden (an beide Topics)
        Map<String, Object> timerStartMsg = Map.of(
                "type", "TIMER_START",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "timeLimit", ROUND_TIME_LIMIT_SECONDS,
                "message", "Timer gestartet - " + ROUND_TIME_LIMIT_SECONDS + " Sekunden"
        );
        
        // An Game-Topic senden (für handleGameMessage)
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId,
                timerStartMsg
        );
        
        // Auch an Timer-Topic senden (für direkte Timer-Updates)
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/timer",
                timerStartMsg
        );

        // Timer starten
        ScheduledFuture<?> timer = timerExecutor.schedule(() -> {
            try {
                log.warn("⏰ TIMER ABGELAUFEN für Spiel {} Runde {}", gameId, roundNumber);
                
                // Game frisch aus DB laden (kann sich geändert haben)
                Game currentGame = gameService.getGameById(gameId);
                
                // Prüfe ob beide Spieler geantwortet haben
                Long player1Id = currentGame.getPlayer1().getId();
                Long player2Id = currentGame.getPlayer2().getId();
                
                String answer1 = playerAnswers.get(gameId + player1Id);
                String answer2 = playerAnswers.get(gameId + player2Id);
                
                // Wenn Spieler noch nicht geantwortet hat, setze Antwort auf "TIMEOUT"
                if (answer1 == null) {
                    log.info("⏰ Spieler {} hat nicht geantwortet - setze Timeout", player1Id);
                    playerAnswers.put(gameId + player1Id, "TIMEOUT");
                    
                    // Benachrichtige Spieler 1 über Timeout
                    sendTimeoutNotification(player1Id, gameId, roundNumber);
                }
                
                if (answer2 == null) {
                    log.info("⏰ Spieler {} hat nicht geantwortet - setze Timeout", player2Id);
                    playerAnswers.put(gameId + player2Id, "TIMEOUT");
                    
                    // Benachrichtige Spieler 2 über Timeout
                    sendTimeoutNotification(player2Id, gameId, roundNumber);
                }
                
                // Prüfe ob jetzt beide geantwortet haben (oder Timeout)
                if (playerAnswers.containsKey(gameId + player1Id) && 
                    playerAnswers.containsKey(gameId + player2Id)) {
                    String finalAnswer1 = playerAnswers.get(gameId + player1Id);
                    String finalAnswer2 = playerAnswers.get(gameId + player2Id);
                    
                    // Punkte berechnen (Timeout = falsch)
                    calculateRoundPoints(currentGame, roundNumber, 
                            finalAnswer1.equals("TIMEOUT") ? "X" : finalAnswer1,
                            finalAnswer2.equals("TIMEOUT") ? "X" : finalAnswer2);
                }
                
                // Timer aus Map entfernen
                roundTimers.remove(timerKey);
                
            } catch (Exception e) {
                log.error("❌ Fehler beim Timer-Ablauf: {}", e.getMessage(), e);
            }
        }, ROUND_TIME_LIMIT_SECONDS, TimeUnit.SECONDS);

        roundTimers.put(timerKey, timer);

        // Timer-Updates jede Sekunde senden (für Countdown)
        final long startTime = System.currentTimeMillis();
        timerExecutor.scheduleAtFixedRate(() -> {
            try {
                ScheduledFuture<?> currentTimer = roundTimers.get(timerKey);
                if (currentTimer != null && !currentTimer.isDone()) {
                    long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                    long remaining = ROUND_TIME_LIMIT_SECONDS - elapsed;
                    
                    if (remaining > 0) {
                        Map<String, Object> timerUpdate = Map.of(
                                "type", "TIMER_UPDATE",
                                "gameId", gameId,
                                "roundNumber", roundNumber,
                                "remainingSeconds", remaining,
                                "message", remaining + " Sekunden verbleibend"
                        );
                        
                        messagingTemplate.convertAndSend(
                                "/topic/game/" + gameId + "/timer",
                                timerUpdate
                        );
                    }
                }
            } catch (Exception e) {
                log.error("❌ Fehler beim Timer-Update: {}", e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Stoppt den Timer für eine Runde.
     */
    private void stopRoundTimer(Long gameId, int roundNumber) {
        String timerKey = gameId + "_" + roundNumber;
        ScheduledFuture<?> timer = roundTimers.remove(timerKey);
        
        if (timer != null && !timer.isDone()) {
            timer.cancel(false);
            log.info("⏱️ Timer gestoppt für Spiel {} Runde {}", gameId, roundNumber);
        }
    }

    /**
     * Sendet eine Timeout-Benachrichtigung an einen Spieler.
     */
    private void sendTimeoutNotification(Long playerId, Long gameId, int roundNumber) {
        Map<String, Object> timeoutMsg = Map.of(
                "type", "ANSWER_TIMEOUT",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "message", "⏰ Zeit abgelaufen! Deine Antwort wurde als falsch gewertet."
        );

        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/game.timeout",
                timeoutMsg
        );
    }

    // ========== BENACHRICHTIGUNGS-METHODEN ==========

    /**
     * Sendet Feedback über die eigene Antwort an einen Spieler.
     */
    private void sendAnswerFeedback(Long playerId, Long gameId, int roundNumber,
                                    boolean isCorrect, String givenAnswer, 
                                    String correctAnswer, int points, String message) {
        Map<String, Object> feedback = Map.of(
                "type", "ANSWER_FEEDBACK",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "playerId", playerId,
                "isCorrect", isCorrect,
                "givenAnswer", givenAnswer,
                "correctAnswer", correctAnswer,
                "points", points,
                "message", message
        );

        // Sende sowohl über user-queue als auch über game-topic (für bessere Zuverlässigkeit)
        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/game.answer.feedback",
                feedback
        );

        // Auch über Game-Topic senden (mit playerId-Filter im Frontend)
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/feedback",
                feedback
        );

        log.info("📨 Antwort-Feedback an Spieler {}: {} (Punkte: {})", 
                playerId, isCorrect ? "richtig" : "falsch", points);
    }

    /**
     * Sendet Information über die Antwort des Gegners.
     */
    private void sendOpponentAnswerFeedback(Long playerId, Long gameId, int roundNumber,
                                           boolean opponentCorrect, int opponentPoints, 
                                           String message) {
        Map<String, Object> feedback = Map.of(
                "type", "OPPONENT_ANSWER_FEEDBACK",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "playerId", playerId,
                "opponentCorrect", opponentCorrect,
                "opponentPoints", opponentPoints,
                "message", message
        );

        // Sende sowohl über user-queue als auch über game-topic
        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/game.opponent.feedback",
                feedback
        );

        // Auch über Game-Topic senden
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/feedback",
                feedback
        );
    }

}