package paf_grp_k.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import paf_grp_k.dto.AnswerRequest;
import paf_grp_k.dto.PlayerDTO;
import paf_grp_k.dto.QuestionPublicDTO;
import paf_grp_k.model.Game;
import paf_grp_k.model.Player;
import paf_grp_k.model.Round;

import java.util.Map;

/**
 * Zentrale Komponente für das Senden von spielbezogenen WebSocket/STOMP Events.
 *
 * <p>Diese Klasse kapselt das Versenden von Nachrichten über {@link SimpMessagingTemplate}
 * und stellt semantische Methoden bereit, die konkrete Domain-Events senden
 * (z. B. {@code GAME_START}, {@code ROUND_START}, {@code TIMER_UPDATE}).</p>
 *
 * <p>Kommunikationsmuster:</p>
 * <ul>
 *   <li><b>Topic (Broadcast)</b>: {@code /topic/...} – alle abonnierenden Clients erhalten die Nachricht.</li>
 *   <li><b>User Queue (privat)</b>: {@code /user/{id}/queue/...} – nur ein bestimmter Spieler erhält die Nachricht.</li>
 * </ul>
 *
 * <p>Die Payloads werden als {@link Map} oder DTOs gesendet und vom Frontend
 * typischerweise über das Feld {@code type} ausgewertet.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketNotifier {

    /**
     * Spring-Template zum Versenden von STOMP/WebSocket Nachrichten.
     *
     */
    private final SimpMessagingTemplate ws;

    // ---------- GAME ----------

    /**
     * Broadcastet das Start-Event eines Spiels.
     *
     * <p>Enthält Basisdaten zum Spiel (Spieler, Kategorie) sowie die erste Runde
     * inklusive öffentlich sichtbarer Frage und Zeitlimit.</p>
     *
     * @param game gestartetes Spiel
     * @param firstRound erste Runde des Spiels
     * @param timeLimitSeconds Zeitlimit pro Runde in Sekunden (wird als Millisekunden gesendet)
     */
    public void broadcastGameStart(Game game, Round firstRound, int timeLimitSeconds) {
        gameTopic(game.getId(), Map.of(
                "type", "GAME_START",
                "gameId", game.getId(),
                "message", "Spiel gestartet!",
                "player1", toPlayerDto(game.getPlayer1()),
                "player2", toPlayerDto(game.getPlayer2()),
                "category", game.getCategory(),
                "round", Map.of(
                        "number", 1,
                        "question", toPublicDto(firstRound.getQuestion()),
                        "timeLimit", timeLimitSeconds * 1000
                ),
                "totalRounds", 5
        ));
    }

    /**
     * Broadcastet den Start einer bestimmten Runde.
     *
     * <p>Enthält die Rundennummer, die öffentlich sichtbaren Fragedaten und das Zeitlimit.</p>
     *
     * @param game zugehöriges Spiel
     * @param roundNumber Nummer der Runde
     * @param round Runden-Entity (inkl. Frage)
     * @param timeLimitSeconds Zeitlimit pro Runde in Sekunden (wird als Millisekunden gesendet)
     */
    public void broadcastRoundStart(Game game, int roundNumber, Round round, int timeLimitSeconds) {
        gameTopic(game.getId(), Map.of(
                "type", "ROUND_START",
                "gameId", game.getId(),
                "roundNumber", roundNumber,
                "question", toPublicDto(round.getQuestion()),
                "timeLimit", timeLimitSeconds * 1000,
                "totalRounds", 5
        ));
    }

    /**
     * Broadcastet das Ergebnis einer Runde an ein dediziertes Result-Topic.
     *
     * <p>Wird typischerweise nach Punkteberechnung gesendet.</p>
     *
     * @param gameId ID des Spiels
     * @param payload Payload-Objekt (Map/DTO) mit Rundenergebnis und Scores
     */
    public void broadcastRoundResult(Long gameId, Object payload) {
        topic("/topic/game/" + gameId + "/result", payload);
    }

    /**
     * Broadcastet ein Spielende-Event an das Game-Topic.
     *
     * @param gameId ID des Spiels
     * @param payload Payload-Objekt (Map/DTO) mit Endstand und Gewinner-Infos
     */
    public void broadcastGameEnd(Long gameId, Object payload) {
        gameTopic(gameId, payload);
    }

    // ---------- ANSWERS ----------

    /**
     * Bestätigt dem antwortenden Spieler, dass seine Antwort serverseitig empfangen wurde.
     *
     * <p>Sendet primär an die private User-Queue {@code /queue/game.answer.confirmed}.
     * Zusätzlich wird der Payload aktuell auch an das Game-Topic gebroadcastet
     * (optional – abhängig davon, ob das Frontend das braucht).</p>
     *
     * @param req eingehende Antwortdaten
     */
    public void sendAnswerConfirmed(AnswerRequest req) {
        var payload = Map.of(
                "type", "ANSWER_CONFIRMED",
                "gameId", req.getGameId(),
                "roundNumber", req.getRoundNumber(),
                "answer", req.getAnswer(),
                "message", "📝 Deine Antwort wurde gesendet: \"" + req.getAnswer() + "\""
        );

        user(req.getPlayerId(), "/queue/game.answer.confirmed", payload);

        // Optional: nur sinnvoll, wenn Clients global diese Info benötigen
        gameTopic(req.getGameId(), payload);
    }

    /**
     * Informiert den Gegner, dass der Spieler bereits geantwortet hat.
     *
     * <p>Es wird keine Antwort übertragen, sondern nur der Hinweis,
     * dass der Gegner "ready" ist (z. B. für UI: Haken/Indikator).</p>
     *
     * @param game aktuelles Spiel
     * @param playerId ID des Spielers, der geantwortet hat
     * @param roundNumber Nummer der Runde
     */
    public void notifyOpponentAnswered(Game game, Long playerId, int roundNumber) {
        Long opp = opponentId(game, playerId);
        if (opp == null) return;

        user(opp, "/queue/game.opponent.answered", Map.of(
                "type", "OPPONENT_ANSWERED",
                "gameId", game.getId(),
                "roundNumber", roundNumber
        ));
    }

    /**
     * Sendet einem Spieler eine Timeout-Nachricht für eine Runde.
     *
     * <p>Wird typischerweise gesendet, wenn der Rundentimer abgelaufen ist
     * und keine Antwort vom Spieler vorlag.</p>
     *
     * @param playerId ID des betroffenen Spielers
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     */
    public void sendTimeout(Long playerId, Long gameId, int roundNumber) {
        user(playerId, "/queue/game.timeout", Map.of(
                "type", "ANSWER_TIMEOUT",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "message", "⏰ Zeit abgelaufen! Deine Antwort wurde als falsch gewertet."
        ));
    }

    // ---------- LOBBY ----------

    /**
     * Sendet einem Spieler den aktuellen Lobby-Status (privat).
     *
     * @param playerId ID des Spielers
     * @param lobbyStatus Statusobjekt (z. B. {@link paf_grp_k.dto.LobbyStatusDTO})
     */
    public void sendLobbyStatus(Long playerId, Object lobbyStatus) {
        user(playerId, "/queue/lobby.status", lobbyStatus);
    }

    /**
     * Benachrichtigt einen Spieler, dass ein Match gefunden wurde.
     *
     * <p>Wird an ein spielerspezifisches Topic gesendet.</p>
     *
     * @param playerId ID des Spielers
     * @param matchMsg Match-Payload (z. B. {@link GameMatchMessage})
     */
    public void sendMatchFound(Long playerId, Object matchMsg) {
        topic("/topic/game/match/player/" + playerId, matchMsg);
    }

    /**
     * Broadcastet ein Lobby-Update an alle Clients, die die Kategorie-Lobby abonniert haben.
     *
     * @param category Lobby-Kategorie
     * @param payload Payload mit Lobby-Infos (Anzahl, IDs, Timestamp, etc.)
     */
    public void broadcastLobbyUpdate(String category, Object payload) {
        topic("/topic/lobby/" + category, payload);
    }

    /**
     * Broadcastet einen Countdown vor Spielstart.
     *
     * @param gameId ID des Spiels
     * @param seconds verbleibende Sekunden bis Spielstart
     */
    public void broadcastCountdown(Long gameId, int seconds) {
        topic("/topic/game/" + gameId + "/countdown", Map.of(
                "type", "COUNTDOWN",
                "gameId", gameId,
                "seconds", seconds,
                "message", "Spiel startet in " + seconds + " Sekunden..."
        ));
    }

    // ---------- TIMER ----------

    /**
     * Broadcastet den Start eines Rundentimers.
     *
     * <p>Es wird sowohl an das Game-Topic als auch an ein dediziertes Timer-Topic gesendet.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     * @param seconds Zeitlimit in Sekunden
     */
    public void broadcastTimerStart(Long gameId, int roundNumber, int seconds) {
        var msg = Map.of(
                "type", "TIMER_START",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "timeLimit", seconds,
                "message", "Timer gestartet - " + seconds + " Sekunden"
        );
        gameTopic(gameId, msg);
        topic("/topic/game/" + gameId + "/timer", msg);
    }

    /**
     * Broadcastet ein Timer-Update (Countdown) für eine Runde.
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     * @param remainingSeconds verbleibende Sekunden
     */
    public void broadcastTimerUpdate(Long gameId, int roundNumber, long remainingSeconds) {
        topic("/topic/game/" + gameId + "/timer", Map.of(
                "type", "TIMER_UPDATE",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "remainingSeconds", remainingSeconds,
                "message", remainingSeconds + " Sekunden verbleibend"
        ));
    }

    // ---------- ERROR ----------

    /**
     * Sendet eine standardisierte Fehlermeldung an einen Spieler (privat).
     *
     * @param playerId ID des Spielers
     * @param message Fehlermeldungstext
     */
    public void sendError(Long playerId, String message) {
        user(playerId, "/queue/errors", Map.of(
                "type", "ERROR",
                "message", message,
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ---------- DEBUG (optional) ----------

    /**
     * Broadcastet Debug-Informationen zu einem gefundenen Match.
     *
     * <p>Kann entfernt werden, wenn im Projekt nicht genutzt.</p>
     *
     * @param gameId ID des Spiels
     * @param player1Id ID von Spieler 1
     * @param player2Id ID von Spieler 2
     */
    public void broadcastDebugMatch(Long gameId, Long player1Id, Long player2Id) {
        topic("/topic/debug/match", Map.of(
                "type", "DEBUG_MATCH",
                "gameId", gameId,
                "player1Id", player1Id,
                "player2Id", player2Id,
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ---------- helpers ----------

    /**
     * Sendet einen Payload an ein beliebiges Topic-Ziel.
     *
     * @param destination Zielpfad (z. B. {@code /topic/game/1})
     * @param payload Objekt/Map, das serialisiert gesendet wird
     */
    private void topic(String destination, Object payload) {
        ws.convertAndSend(destination, payload);
    }

    /**
     * Sendet einen Payload an das Standard-Game-Topic eines Spiels.
     *
     * @param gameId ID des Spiels
     * @param payload Payload
     */
    private void gameTopic(Long gameId, Object payload) {
        topic("/topic/game/" + gameId, payload);
    }

    /**
     * Sendet einen Payload an die private User-Destination eines Spielers.
     *
     * @param userId ID des Spielers
     * @param destination Zielpfad innerhalb von {@code /user/...} (z. B. {@code /queue/errors})
     * @param payload Payload
     */
    private void user(Long userId, String destination, Object payload) {
        ws.convertAndSendToUser(userId.toString(), destination, payload);
    }

    /**
     * Ermittelt die Gegner-ID für einen Spieler innerhalb eines Spiels.
     *
     * @param game Spiel
     * @param playerId ID des aktuellen Spielers
     * @return ID des Gegners oder {@code null}, wenn {@code playerId} nicht zum Spiel gehört
     */
    private Long opponentId(Game game, Long playerId) {
        if (game.getPlayer1().getId().equals(playerId)) return game.getPlayer2().getId();
        if (game.getPlayer2().getId().equals(playerId)) return game.getPlayer1().getId();
        return null;
    }

    /**
     * Konvertiert ein {@link Player}-Entity in ein {@link PlayerDTO}.
     *
     * <p>Es werden ausschließlich nicht-sensible Informationen übertragen.</p>
     *
     * @param p Spieler-Entity
     * @return befülltes {@link PlayerDTO}
     */
    private PlayerDTO toPlayerDto(Player p) {
        PlayerDTO dto = new PlayerDTO();
        dto.setId(p.getId());
        dto.setUsername(p.getUsername());
        dto.setProfileImageUrl(p.getProfileImageUrl());
        dto.setTotalGames(p.getTotalGames());
        dto.setGamesWon(p.getGamesWon());
        dto.setGamesLost(p.getGamesLost());
        dto.setHighscore(p.getHighscore());
        return dto;
    }

    /**
     * Konvertiert eine {@link paf_grp_k.model.Question} in ein öffentliches DTO ohne Lösung.
     *
     * <p>Wichtig: Das DTO enthält keine korrekte Antwort, damit Clients nicht cheaten können.</p>
     *
     * @param q Question-Entity
     * @return {@link QuestionPublicDTO} für den Client
     */
    private QuestionPublicDTO toPublicDto(paf_grp_k.model.Question q) {
        return new QuestionPublicDTO(
                q.getId(), q.getCategory(), q.getQuestionText(),
                q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD()
        );
    }
}
