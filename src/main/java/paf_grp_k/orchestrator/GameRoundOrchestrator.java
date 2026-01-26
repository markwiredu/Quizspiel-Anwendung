package paf_grp_k.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import paf_grp_k.dto.AnswerRequest;
import paf_grp_k.dto.NextRoundRequest;
import paf_grp_k.model.Game;
import paf_grp_k.model.Round;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.service.GameService;
import paf_grp_k.service.RoundTimerService;
import paf_grp_k.websocket.GameWebSocketNotifier;
import org.springframework.beans.factory.annotation.Value;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GameRoundOrchestrator {

    private final GameService gameService;
    private final PlayerRepository playerRepository;
    private final RoundTimerService timerService;
    private final GameWebSocketNotifier notifier;
    @Value("${game.debug.auto-timeout-opponent:false}")
    private boolean autoTimeoutOpponent;


    public GameRoundOrchestrator(GameService gameService,
                                 PlayerRepository playerRepository,
                                 RoundTimerService timerService,
                                 GameWebSocketNotifier notifier) {
        this.gameService = gameService;
        this.playerRepository = playerRepository;
        this.timerService = timerService;
        this.notifier = notifier;
    }

    // Antworten (thread-safe)
    private final Map<String, String> playerAnswers = new ConcurrentHashMap<>();

    // Einheitlicher Key pro Spiel+Spieler (kein "key(...)" verwenden -> Konflikte vermeiden)
    private String answerKey(Long gameId, Long playerId) {
        return gameId + ":" + playerId;
    }

    // ===== Answer Handling =====

    public void onAnswer(AnswerRequest request) {
        try {
            if (request.getGameId() == null) {
                notifier.sendError(request.getPlayerId(), "Game-ID fehlt in der Antwort");
                return;
            }

            Game game = gameService.getGameById(request.getGameId());

            log.info("✅ onAnswer erreicht: gameId={}, playerId={}, round={}, answer={}, autoTimeoutOpponent={}",
                    request.getGameId(), request.getPlayerId(), request.getRoundNumber(), request.getAnswer(), autoTimeoutOpponent);

            // ✅ nur erste Antwort pro Spieler/Runde zählen
            String myKey = answerKey(request.getGameId(), request.getPlayerId());
            if (playerAnswers.containsKey(myKey)) {
                log.warn("⚠️ Spieler {} hat bereits geantwortet - zweite Antwort ignoriert", request.getPlayerId());
                return;
            }

            // 1) Antwort speichern
            playerAnswers.put(myKey, request.getAnswer());

            // 2) DEBUG: Gegner sofort als TIMEOUT setzen (für Single-Player Test)
            if (autoTimeoutOpponent) {
                Long opponentId = game.getPlayer1().getId().equals(request.getPlayerId())
                        ? game.getPlayer2().getId()
                        : game.getPlayer1().getId();

                playerAnswers.putIfAbsent(answerKey(request.getGameId(), opponentId), "TIMEOUT");
                log.info("🧪 DEBUG: Gegner sofort TIMEOUT gesetzt: opponentId={}", opponentId);
            }

            // 3) Bestätigung senden
            notifier.sendAnswerConfirmed(request);

            // 4) Gegner informieren (wenn vorhanden)
            notifier.notifyOpponentAnswered(game, request.getPlayerId(), request.getRoundNumber());

            // 5) check -> wenn beide da sind, wird calculateRoundPoints aufgerufen
            checkIfBothAnswered(game, request.getRoundNumber());

        } catch (Exception e) {
            log.error("❌ Fehler bei onAnswer: {}", e.getMessage(), e);
        }
    }


    // ===== Next Round =====

    public void startNextRound(NextRoundRequest request) {
        try {
            Game game = gameService.getGameById(request.getGameId());

            // alten Timer stoppen
            timerService.stopTimer(request.getGameId(), request.getRoundNumber() - 1);

            // neue Runde starten
            Round round = gameService.startNewRound(request.getGameId(), request.getRoundNumber());

            // Frage broadcasten
            notifier.broadcastRoundStart(game, request.getRoundNumber(), round, timerService.getRoundTimeLimitSeconds());

            // Antworten resetten
            playerAnswers.remove(answerKey(request.getGameId(), game.getPlayer1().getId()));
            playerAnswers.remove(answerKey(request.getGameId(), game.getPlayer2().getId()));

            // Timer starten mit Callback
            timerService.startTimer(
                    request.getGameId(),
                    request.getRoundNumber(),
                    () -> onRoundTimeout(request.getGameId(), request.getRoundNumber())
            );

        } catch (Exception e) {
            log.error("❌ Fehler bei startNextRound: {}", e.getMessage(), e);
        }
    }

    // ===== Timeout Handling =====

    public void onRoundTimeout(Long gameId, int roundNumber) {
        try {
            Game currentGame = gameService.getGameById(gameId);

            Long p1 = currentGame.getPlayer1().getId();
            Long p2 = currentGame.getPlayer2().getId();

            // fehlende Antworten als TIMEOUT setzen
            playerAnswers.putIfAbsent(answerKey(gameId, p1), "TIMEOUT");
            playerAnswers.putIfAbsent(answerKey(gameId, p2), "TIMEOUT");

            // Timeout Nachricht nur schicken, wenn Spieler wirklich Timeout hat
            if ("TIMEOUT".equals(playerAnswers.get(answerKey(gameId, p1)))) {
                notifier.sendTimeout(p1, gameId, roundNumber);
            }
            if ("TIMEOUT".equals(playerAnswers.get(answerKey(gameId, p2)))) {
                notifier.sendTimeout(p2, gameId, roundNumber);
            }

            // prüfen ob beide nun eine Antwort haben (Antwort oder Timeout)
            checkIfBothAnswered(currentGame, roundNumber);

        } catch (Exception e) {
            log.error("❌ Fehler bei onRoundTimeout: {}", e.getMessage(), e);
        }
    }

    // ===== Round Finished Check =====

    private void checkIfBothAnswered(Game game, int roundNumber) {
        Long p1 = game.getPlayer1().getId();
        Long p2 = game.getPlayer2().getId();

        String a1 = playerAnswers.get(answerKey(game.getId(), p1));
        String a2 = playerAnswers.get(answerKey(game.getId(), p2));

        if (a1 != null && a2 != null) {
            // ✅ wichtig: Timer stoppen sobald beide Antworten da sind


            calculateRoundPoints(game, roundNumber, a1, a2);

            // cleanup
            playerAnswers.remove(answerKey(game.getId(), p1));
            playerAnswers.remove(answerKey(game.getId(), p2));
        }
    }

    // ===== Scoring =====
    private void calculateRoundPoints(Game game, int roundNumber, String answer1, String answer2) {
        timerService.stopTimer(game.getId(), roundNumber);

        // DEBUG: du siehst sofort, dass Auswertung passiert
        log.info("✅ calculateRoundPoints: gameId={}, round={}, a1={}, a2={}",
                game.getId(), roundNumber, answer1, answer2);

        // Beispiel: richtige Antwort aus DB (passe Methodennamen ggf. an!)
        Round round = gameService.getCurrentRound(game.getId(), roundNumber);
        String correct = round.getQuestion().getCorrectAnswer(); // oft "A"/"B"/"C"/"D"

        if ("TIMEOUT".equalsIgnoreCase(answer1)) answer1 = "X";
        if ("TIMEOUT".equalsIgnoreCase(answer2)) answer2 = "X";

        int p1Points = correct.equalsIgnoreCase(answer1) ? 10 : 0;
        int p2Points = correct.equalsIgnoreCase(answer2) ? 10 : 0;

        game = gameService.addScores(game.getId(), p1Points, p2Points);


        notifier.broadcastRoundResult(game.getId(), Map.of(
                "type", "ROUND_RESULT",
                "result", Map.of(
                        "gameId", game.getId(),
                        "roundNumber", roundNumber,
                        "correctAnswer", correct,
                        "player1Points", p1Points,
                        "player2Points", p2Points,
                        "message", "Runde " + roundNumber + " beendet!",
                        "gameFinished", roundNumber >= 5
                ),
                "scores", Map.of(
                        "player1", game.getScorePlayer1(),
                        "player2", game.getScorePlayer2()
                )
        ));

// ✅ NUR EINMAL prüfen und dann finishen
        if (roundNumber >= 5) {
            finishGame(game);
        }
    }



        private void finishGame(Game game) {
        // 1) Spiel in DB als FINISHED markieren + winner setzen (macht dein GameService bereits)
        Game finished = gameService.finishGame(game.getId());

        // 2) GAME_END an alle Clients senden
        notifier.broadcastGameEnd(finished.getId(), Map.of(
                "type", "GAME_END",
                "gameId", finished.getId(),
                "scores", Map.of(
                        "player1", finished.getScorePlayer1(),
                        "player2", finished.getScorePlayer2()
                ),
                "winnerId", finished.getWinner() != null ? finished.getWinner().getId() : null,
                "message", "Spiel beendet!"
        ));
    }


    // ===== Game Start =====

    public void startFirstRound(Long gameId) {
        try {
            Game game = gameService.getGameById(gameId);

            // Runde 1 erzeugen
            Round firstRound = gameService.startNewRound(gameId, 1);

            // GAME_START senden
            notifier.broadcastGameStart(game, firstRound, timerService.getRoundTimeLimitSeconds());

            // ROUND_START senden
            notifier.broadcastRoundStart(game, 1, firstRound, timerService.getRoundTimeLimitSeconds());

            // Antworten resetten
            playerAnswers.remove(answerKey(gameId, game.getPlayer1().getId()));
            playerAnswers.remove(answerKey(gameId, game.getPlayer2().getId()));

            // Timer starten mit Callback
            timerService.startTimer(gameId, 1, () -> onRoundTimeout(gameId, 1));

        } catch (Exception e) {
            log.error("❌ Fehler bei startFirstRound: {}", e.getMessage(), e);
        }
    }

    public void syncRound(Long gameId, int roundNumber) {
        try {
            Game game = gameService.getGameById(gameId);

            // aktuelle Runde aus DB holen (du hattest so eine Methode früher schon)
            Round round = gameService.getCurrentRound(gameId, roundNumber);

            // Runde nochmal senden, damit der Client die Frage bekommt
            notifier.broadcastRoundStart(game, roundNumber, round, timerService.getRoundTimeLimitSeconds());

            // optional: Timer-Start nochmal schicken (hilft wenn TIMER_START verpasst wurde)
            notifier.broadcastTimerStart(gameId, roundNumber, timerService.getRoundTimeLimitSeconds());

            log.info("🔄 Sync gesendet für Spiel {} Runde {}", gameId, roundNumber);
        } catch (Exception e) {
            log.error("❌ Fehler bei syncRound: {}", e.getMessage(), e);
        }
    }

}
