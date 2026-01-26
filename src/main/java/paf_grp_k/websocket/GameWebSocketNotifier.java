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

@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    // =========================
    // GAME START
    // =========================

    public void broadcastGameStart(Game game, Round firstRound, int timeLimitSeconds) {
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), Map.of(
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

    // =========================
    // ROUND START
    // =========================

    public void broadcastRoundStart(Game game, int roundNumber, Round round, int timeLimitSeconds) {
        Map<String, Object> msg = Map.of(
                "type", "ROUND_START",
                "gameId", game.getId(),
                "roundNumber", roundNumber,
                "question", toPublicDto(round.getQuestion()),
                "timeLimit", timeLimitSeconds * 1000,
                "totalRounds", 5
        );

        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), msg);
    }




    // =========================
    // ANSWER FLOW
    // =========================

    public void sendAnswerConfirmed(AnswerRequest request) {
        Map<String, Object> payload = Map.of(
                "type", "ANSWER_CONFIRMED",
                "gameId", request.getGameId(),
                "roundNumber", request.getRoundNumber(),
                "answer", request.getAnswer(),
                "message", "📝 Deine Antwort wurde gesendet: \"" + request.getAnswer() + "\""
        );

        // user-queue
        messagingTemplate.convertAndSendToUser(
                request.getPlayerId().toString(),
                "/queue/game.answer.confirmed",
                payload
        );

        // ✅ zusätzlich ins Game-Topic (Debug/Robustheit)
        messagingTemplate.convertAndSend("/topic/game/" + request.getGameId(), payload);
    }


    public void notifyOpponentAnswered(Game game, Long playerId, int roundNumber) {
        Long opponentId = getOpponentId(game, playerId);
        if (opponentId == null) return;

        messagingTemplate.convertAndSendToUser(
                opponentId.toString(),
                "/queue/game.opponent.answered",
                Map.of(
                        "type", "OPPONENT_ANSWERED",
                        "gameId", game.getId(),
                        "roundNumber", roundNumber
                )
        );
    }

    public void sendTimeout(Long playerId, Long gameId, int roundNumber) {
        messagingTemplate.convertAndSendToUser(
                playerId.toString(),
                "/queue/game.timeout",
                Map.of(
                        "type", "ANSWER_TIMEOUT",
                        "gameId", gameId,
                        "roundNumber", roundNumber,
                        "message", "⏰ Zeit abgelaufen! Deine Antwort wurde als falsch gewertet."
                )
        );
    }

    // =========================
    // LOBBY / MATCH
    // =========================

    public void sendLobbyStatus(Long playerId, Object lobbyStatus) {
        messagingTemplate.convertAndSendToUser(playerId.toString(), "/queue/lobby.status", lobbyStatus);
    }

    public void sendMatchFound(Long playerId, Object matchMsg) {
        messagingTemplate.convertAndSend("/topic/game/match/player/" + playerId, matchMsg);
    }

    public void broadcastLobbyUpdate(String category, Object payload) {
        messagingTemplate.convertAndSend("/topic/lobby/" + category, payload);
    }

    public void broadcastCountdown(Long gameId, int seconds) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/countdown", Map.of(
                "type", "COUNTDOWN",
                "gameId", gameId,
                "seconds", seconds,
                "message", "Spiel startet in " + seconds + " Sekunden..."
        ));
    }

    // =========================
    // TIMER
    // =========================

    public void broadcastTimerStart(Long gameId, int roundNumber, int seconds) {
        Map<String, Object> msg = Map.of(
                "type", "TIMER_START",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "timeLimit", seconds,
                "message", "Timer gestartet - " + seconds + " Sekunden"
        );
        messagingTemplate.convertAndSend("/topic/game/" + gameId, msg);
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/timer", msg);
    }

    public void broadcastTimerUpdate(Long gameId, int roundNumber, long remainingSeconds) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/timer", Map.of(
                "type", "TIMER_UPDATE",
                "gameId", gameId,
                "roundNumber", roundNumber,
                "remainingSeconds", remainingSeconds,
                "message", remainingSeconds + " Sekunden verbleibend"
        ));
    }

    // =========================
    // ERROR
    // =========================

    public void sendError(Long playerId, String message) {
        messagingTemplate.convertAndSendToUser(playerId.toString(), "/queue/errors", Map.of(
                "type", "ERROR",
                "message", message,
                "timestamp", System.currentTimeMillis()
        ));
    }

    // =========================
    // HELPERS
    // =========================

    public void broadcastDebugMatch(Long gameId, Long player1Id, Long player2Id) {
        messagingTemplate.convertAndSend("/topic/debug/match", Map.of(
                "type", "DEBUG_MATCH",
                "gameId", gameId,
                "player1Id", player1Id,
                "player2Id", player2Id,
                "timestamp", System.currentTimeMillis()
        ));
    }

    public void broadcastRoundResult(Long gameId, Object payload) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId + "/result", payload);
    }



    private Long getOpponentId(Game game, Long playerId) {
        if (game.getPlayer1().getId().equals(playerId)) {
            return game.getPlayer2().getId();
        } else if (game.getPlayer2().getId().equals(playerId)) {
            return game.getPlayer1().getId();
        }
        return null;
    }

    public void broadcastGameEnd(Long gameId, Object payload) {
        messagingTemplate.convertAndSend("/topic/game/" + gameId, payload);
    }


    private PlayerDTO toPlayerDto(Player player) {
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
}
