package paf_grp_k.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import paf_grp_k.dto.AnswerRequest;
import paf_grp_k.dto.NextRoundRequest;
import paf_grp_k.model.Game;
import paf_grp_k.model.Round;
import paf_grp_k.service.GameService;
import paf_grp_k.service.RoundTimerService;
import paf_grp_k.websocket.GameWebSocketNotifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrator für den Ablauf rundenbasierter Spiele über WebSockets.
 *
 * <p>Diese Klasse koordiniert den kompletten Runden-Lifecycle:</p>
 * <ul>
 *   <li>Entgegennahme von Spielerantworten</li>
 *   <li>Timer-Start/-Stop und Timeout-Behandlung</li>
 *   <li>Auswertung der Runde und Punktevergabe</li>
 *   <li>Broadcast von Round- und Game-Events an Clients</li>
 * </ul>
 *
 * <p>Wichtig: Die eigentliche Persistenz- und Spielzustandslogik liegt im {@link GameService},
 * die WebSocket-Ausgabe im {@link GameWebSocketNotifier}. Dieser Orchestrator verbindet beides
 * und sorgt für den korrekten Ablauf.</p>
 *
 * <p>Thread-Safety: Spielerantworten werden in einer {@link ConcurrentHashMap} gespeichert,
 * da Antworten und Timer-Callbacks parallel eintreffen können.</p>
 */
@Service
@Slf4j
public class GameRoundOrchestrator {

    /**
     * Marker-Wert für ausbleibende Antworten (Timeout).
     */
    private static final String TIMEOUT = "TIMEOUT";

    /**
     * Maximale Anzahl an Runden pro Spiel.
     */
    private static final int MAX_ROUNDS = 5;

    /**
     * Service für Spielzustand (Rundenstart, Punktestand, Abschluss, Persistenz).
     */
    private final GameService gameService;

    /**
     * Service zur Verwaltung von Rundentimern (Start/Stop/Callback).
     */
    private final RoundTimerService timerService;

    /**
     * Komponente zum Senden/Broadcasten von WebSocket-Events an Clients.
     */
    private final GameWebSocketNotifier notifier;

    /**
     * Debug-Feature: setzt den Gegner automatisch auf Timeout, um Solo-Tests zu erleichtern.
     * Default: {@code false}. Aktivierbar via Property {@code game.debug.auto-timeout-opponent}.
     */
    @Value("${game.debug.auto-timeout-opponent:false}")
    private boolean autoTimeoutOpponent;

    /**
     * Zwischenspeicher für Antworten pro Spiel und Spieler.
     *
     * <p>Key-Format: {@code "{gameId}:{playerId}"}.</p>
     */
    private final Map<String, String> playerAnswers = new ConcurrentHashMap<>();

    /**
     * Erstellt einen neuen Orchestrator mit seinen Abhängigkeiten.
     *
     * @param gameService Service für Spielzustandsänderungen
     * @param timerService Service zur Timer-Steuerung
     * @param notifier WebSocket-Notifier für Server-zu-Client Events
     */
    public GameRoundOrchestrator(GameService gameService,
                                 RoundTimerService timerService,
                                 GameWebSocketNotifier notifier) {
        this.gameService = gameService;
        this.timerService = timerService;
        this.notifier = notifier;
    }

    /**
     * Erzeugt den Map-Key für eine Spielerantwort in einem bestimmten Spiel.
     *
     * @param gameId ID des Spiels
     * @param playerId ID des Spielers
     * @return zusammengesetzter Schlüssel im Format {@code "{gameId}:{playerId}"}
     */
    private String answerKey(Long gameId, Long playerId) {
        return gameId + ":" + playerId;
    }

    /**
     * Entfernt gespeicherte Antworten beider Spieler für ein Spiel.
     *
     * <p>Wird genutzt, um nach Auswertung einer Runde oder beim Start
     * einer neuen Runde einen sauberen Zustand herzustellen.</p>
     *
     * @param gameId ID des Spiels
     * @param p1 ID von Spieler 1
     * @param p2 ID von Spieler 2
     */
    private void resetAnswers(Long gameId, Long p1, Long p2) {
        playerAnswers.remove(answerKey(gameId, p1));
        playerAnswers.remove(answerKey(gameId, p2));
    }

    /**
     * Stellt sicher, dass für einen Spieler eine Timeout-Antwort gesetzt ist,
     * falls keine Antwort vorhanden ist.
     *
     * @param gameId ID des Spiels
     * @param playerId ID des Spielers
     */
    private void ensureTimeoutIfMissing(Long gameId, Long playerId) {
        playerAnswers.putIfAbsent(answerKey(gameId, playerId), TIMEOUT);
    }

    /**
     * Sendet eine Timeout-Notification an den Client, wenn der gespeicherte Wert
     * tatsächlich {@link #TIMEOUT} ist.
     *
     * @param gameId ID des Spiels
     * @param roundNumber aktuelle Runde
     * @param playerId ID des Spielers
     */
    private void sendTimeoutIfTimedOut(Long gameId, int roundNumber, Long playerId) {
        if (TIMEOUT.equals(playerAnswers.get(answerKey(gameId, playerId)))) {
            notifier.sendTimeout(playerId, gameId, roundNumber);
        }
    }

    // ===== Answer Handling =====

    /**
     * Verarbeitet eine eingehende Spielerantwort.
     *
     * <p>Ablauf:</p>
     * <ol>
     *   <li>Validierung der Request-Daten</li>
     *   <li>Verhindern von Mehrfachantworten pro Spieler und Runde</li>
     *   <li>Speichern der Antwort (thread-safe)</li>
     *   <li>Optional: Debug-Timeout für Gegner setzen</li>
     *   <li>Bestätigung an den Antwortenden senden</li>
     *   <li>Gegner über abgegebene Antwort informieren</li>
     *   <li>Wenn beide Antworten vorhanden sind: Runde auswerten</li>
     * </ol>
     *
     * @param request Antwortdaten des Spielers
     */
    public void onAnswer(AnswerRequest request) {
        try {
            if (request.getGameId() == null) {
                notifier.sendError(request.getPlayerId(), "Game-ID fehlt in der Antwort");
                return;
            }

            Game game = gameService.getGameById(request.getGameId());

            log.info("✅ onAnswer erreicht: gameId={}, playerId={}, round={}, answer={}, autoTimeoutOpponent={}",
                    request.getGameId(), request.getPlayerId(), request.getRoundNumber(), request.getAnswer(), autoTimeoutOpponent);

            // Nur erste Antwort pro Spieler/Runde zählen
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

                playerAnswers.putIfAbsent(answerKey(request.getGameId(), opponentId), TIMEOUT);
                log.info("🧪 DEBUG: Gegner sofort TIMEOUT gesetzt: opponentId={}", opponentId);
            }

            // 3) Bestätigung senden
            notifier.sendAnswerConfirmed(request);

            // 4) Gegner informieren
            notifier.notifyOpponentAnswered(game, request.getPlayerId(), request.getRoundNumber());

            // 5) Wenn beide da sind, wird ausgewertet
            checkIfBothAnswered(game, request.getRoundNumber());

        } catch (Exception e) {
            log.error("❌ Fehler bei onAnswer: {}", e.getMessage(), e);
        }
    }

    // ===== Next Round =====

    /**
     * Startet eine neue Runde in einem laufenden Spiel.
     *
     * <p>Stoppt zunächst den Timer der vorherigen Runde, legt anschließend
     * eine neue Runde an und broadcastet die neue Frage an beide Clients.
     * Danach werden die Antworten zurückgesetzt und der neue Rundentimer gestartet.</p>
     *
     * @param request Request-Daten mit Spiel-ID und Rundennummer
     */
    public void startNextRound(NextRoundRequest request) {
        try {
            Game game = gameService.getGameById(request.getGameId());

            // Alten Timer stoppen
            timerService.stopTimer(request.getGameId(), request.getRoundNumber() - 1);

            // Neue Runde starten
            Round round = gameService.startNewRound(request.getGameId(), request.getRoundNumber());

            // Frage broadcasten
            notifier.broadcastRoundStart(game, request.getRoundNumber(), round, timerService.getRoundTimeLimitSeconds());

            // Antworten resetten
            resetAnswers(request.getGameId(), game.getPlayer1().getId(), game.getPlayer2().getId());

            // Timer starten mit Timeout-Callback
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

    /**
     * Wird aufgerufen, wenn der Rundentimer abläuft.
     *
     * <p>Setzt für fehlende Antworten automatisch {@link #TIMEOUT}, sendet
     * Timeout-Nachrichten an betroffene Spieler und löst anschließend
     * die Runden-Auswertung aus, sobald beide Spieler entweder geantwortet
     * oder ein Timeout haben.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     */
    public void onRoundTimeout(Long gameId, int roundNumber) {
        try {
            Game currentGame = gameService.getGameById(gameId);

            Long p1 = currentGame.getPlayer1().getId();
            Long p2 = currentGame.getPlayer2().getId();

            // Fehlende Antworten als TIMEOUT setzen
            ensureTimeoutIfMissing(gameId, p1);
            ensureTimeoutIfMissing(gameId, p2);

            // Timeout-Message nur senden, wenn wirklich TIMEOUT
            sendTimeoutIfTimedOut(gameId, roundNumber, p1);
            sendTimeoutIfTimedOut(gameId, roundNumber, p2);

            // Prüfen, ob beide nun "Antwort" oder "Timeout" haben
            checkIfBothAnswered(currentGame, roundNumber);

        } catch (Exception e) {
            log.error("❌ Fehler bei onRoundTimeout: {}", e.getMessage(), e);
        }
    }

    // ===== Round Finished Check =====

    /**
     * Prüft, ob beide Spieler für die Runde eine Antwort (oder Timeout) haben.
     *
     * <p>Sobald beide Werte vorhanden sind, wird die Runde ausgewertet
     * und der Antwortspeicher anschließend zurückgesetzt.</p>
     *
     * @param game aktuelles Spiel
     * @param roundNumber Nummer der Runde
     */
    private void checkIfBothAnswered(Game game, int roundNumber) {
        Long p1 = game.getPlayer1().getId();
        Long p2 = game.getPlayer2().getId();
        Long gameId = game.getId();

        String a1 = playerAnswers.get(answerKey(gameId, p1));
        String a2 = playerAnswers.get(answerKey(gameId, p2));

        if (a1 != null && a2 != null) {
            calculateRoundPoints(game, roundNumber, a1, a2);
            resetAnswers(gameId, p1, p2);
        }
    }

    // ===== Scoring =====

    /**
     * Wertet die Runde aus, vergibt Punkte und broadcastet das Rundenergebnis.
     *
     * <p>Regel: Pro richtiger Antwort gibt es 10 Punkte, ansonsten 0.
     * Timeout wird intern als falsche Antwort behandelt.</p>
     *
     * <p>Nach der Auswertung werden die Spielpunkte persistiert und ein
     * {@code ROUND_RESULT}-Event inklusive aktueller Gesamtscores gebroadcastet.
     * Ab der letzten Runde wird zusätzlich das Spiel beendet.</p>
     *
     * @param game aktuelles Spiel
     * @param roundNumber Nummer der Runde
     * @param answer1 Antwort von Spieler 1 (oder {@link #TIMEOUT})
     * @param answer2 Antwort von Spieler 2 (oder {@link #TIMEOUT})
     */
    private void calculateRoundPoints(Game game, int roundNumber, String answer1, String answer2) {
        timerService.stopTimer(game.getId(), roundNumber);

        log.info("✅ calculateRoundPoints: gameId={}, round={}, a1={}, a2={}",
                game.getId(), roundNumber, answer1, answer2);

        Round round = gameService.getCurrentRound(game.getId(), roundNumber);
        String correct = round.getQuestion().getCorrectAnswer();

        // Timeout als "falsch" behandeln
        answer1 = TIMEOUT.equalsIgnoreCase(answer1) ? "X" : answer1;
        answer2 = TIMEOUT.equalsIgnoreCase(answer2) ? "X" : answer2;

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
                        "gameFinished", roundNumber >= MAX_ROUNDS
                ),
                "scores", Map.of(
                        "player1", game.getScorePlayer1(),
                        "player2", game.getScorePlayer2()
                )
        ));

        if (roundNumber >= MAX_ROUNDS) {
            finishGame(game);
        }
    }

    /**
     * Beendet ein Spiel, persistiert den Abschlusszustand und broadcastet das Endergebnis.
     *
     * <p>Es wird ein {@code GAME_END}-Event gesendet, inklusive finaler Scores
     * und optionaler Gewinner-ID.</p>
     *
     * @param game aktuelles (zu beendendes) Spiel
     */
    private void finishGame(Game game) {
        Game finished = gameService.finishGame(game.getId());

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

    /**
     * Startet die erste Runde eines Spiels.
     *
     * <p>Legt Runde 1 an, sendet ein {@code GAME_START}-Event sowie ein
     * {@code ROUND_START}-Event, setzt den Antwortspeicher zurück
     * und startet den Rundentimer.</p>
     *
     * @param gameId ID des Spiels
     */
    public void startFirstRound(Long gameId) {
        try {
            Game game = gameService.getGameById(gameId);

            Round firstRound = gameService.startNewRound(gameId, 1);

            notifier.broadcastGameStart(game, firstRound, timerService.getRoundTimeLimitSeconds());
            notifier.broadcastRoundStart(game, 1, firstRound, timerService.getRoundTimeLimitSeconds());

            resetAnswers(gameId, game.getPlayer1().getId(), game.getPlayer2().getId());

            timerService.startTimer(gameId, 1, () -> onRoundTimeout(gameId, 1));

        } catch (Exception e) {
            log.error("❌ Fehler bei startFirstRound: {}", e.getMessage(), e);
        }
    }

    /**
     * Synchronisiert den Rundenstatus für Clients (z. B. Reconnect / Late Join).
     *
     * <p>Broadcastet den aktuellen Rundenstart inkl. Frage sowie einen Timer-Start,
     * damit Clients denselben Zustand anzeigen können.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     */
    public void syncRound(Long gameId, int roundNumber) {
        try {
            Game game = gameService.getGameById(gameId);
            Round round = gameService.getCurrentRound(gameId, roundNumber);

            notifier.broadcastRoundStart(game, roundNumber, round, timerService.getRoundTimeLimitSeconds());
            notifier.broadcastTimerStart(gameId, roundNumber, timerService.getRoundTimeLimitSeconds());

            log.info("🔄 Sync gesendet für Spiel {} Runde {}", gameId, roundNumber);
        } catch (Exception e) {
            log.error("❌ Fehler bei syncRound: {}", e.getMessage(), e);
        }
    }
}
