package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import paf_grp_k.dto.AnswerRequest;
import paf_grp_k.dto.JoinLobbyRequest;
import paf_grp_k.dto.NextRoundRequest;
import paf_grp_k.orchestrator.GameRoundOrchestrator;
import paf_grp_k.orchestrator.LobbyMatchOrchestrator;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final LobbyMatchOrchestrator lobbyMatchOrchestrator;
    private final GameRoundOrchestrator gameRoundOrchestrator;

    @MessageMapping("/game.join")
    public void join(@Payload JoinLobbyRequest request) {
        lobbyMatchOrchestrator.join(request);
    }

    @MessageMapping("/game.leave")
    public void leave(@Payload JoinLobbyRequest request) {
        lobbyMatchOrchestrator.leave(request);
    }

    @MessageMapping("/game.answer")
    public void answer(@Payload AnswerRequest request) {
        gameRoundOrchestrator.onAnswer(request);
    }

    @MessageMapping("/game.nextRound")
    public void nextRound(@Payload NextRoundRequest request) {
        gameRoundOrchestrator.startNextRound(request);
    }

    @MessageMapping("/game.sync")
    public void sync(@Payload Map<String, Object> req) {
        Long gameId = Long.valueOf(req.get("gameId").toString());
        int roundNumber = Integer.parseInt(req.get("roundNumber").toString());
        gameRoundOrchestrator.syncRound(gameId, roundNumber);
    }

}
