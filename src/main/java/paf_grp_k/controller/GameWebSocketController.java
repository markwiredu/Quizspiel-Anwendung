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

/**
 * WebSocket-Controller für spielbezogene Echtzeit-Interaktionen.
 *
 * <p>Diese Klasse verarbeitet STOMP-Nachrichten von Clients und leitet
 * sie an die zuständigen Orchestrator-Komponenten weiter. Sie enthält
 * selbst keine Geschäftslogik.</p>
 *
 * <p>Alle Nachrichten werden unter dem Zielpräfix {@code /app} empfangen,
 * z. B. {@code /app/game.join}.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    /**
     * Orchestrator für Lobby- und Matchmaking-Logik.
     */
    private final LobbyMatchOrchestrator lobbyMatchOrchestrator;

    /**
     * Orchestrator für rundenbasierte Spiellogik.
     */
    private final GameRoundOrchestrator gameRoundOrchestrator;

    /**
     * Verarbeitet die Anfrage eines Spielers, einer Lobby beizutreten.
     *
     * @param request Payload mit Lobby- und Spielerinformationen
     */
    @MessageMapping("/game.join")
    public void join(@Payload JoinLobbyRequest request) {
        lobbyMatchOrchestrator.join(request);
    }

    /**
     * Verarbeitet die Anfrage eines Spielers, eine Lobby zu verlassen.
     *
     * @param request Payload mit Lobby- und Spielerinformationen
     */
    @MessageMapping("/game.leave")
    public void leave(@Payload JoinLobbyRequest request) {
        lobbyMatchOrchestrator.leave(request);
    }

    /**
     * Verarbeitet eine Antwort eines Spielers innerhalb einer Spielrunde.
     *
     * <p>Die Antwort wird an den Runden-Orchestrator weitergeleitet,
     * der Bewertung, Punktevergabe und Statusaktualisierung übernimmt.</p>
     *
     * @param request Payload mit Antwortdaten des Spielers
     */
    @MessageMapping("/game.answer")
    public void answer(@Payload AnswerRequest request) {
        gameRoundOrchestrator.onAnswer(request);
    }

    /**
     * Startet die nächste Runde eines laufenden Spiels.
     *
     * <p>Diese Nachricht wird typischerweise vom Host oder
     * nach Abschluss einer Runde gesendet.</p>
     *
     * @param request Payload mit Spiel- und Rundeninformationen
     */
    @MessageMapping("/game.nextRound")
    public void nextRound(@Payload NextRoundRequest request) {
        gameRoundOrchestrator.startNextRound(request);
    }

    /**
     * Synchronisiert den aktuellen Rundenstatus für einen Client.
     *
     * <p>Dieser Endpunkt wird verwendet, wenn ein Client neu beitritt
     * oder den aktuellen Spielzustand erneut anfordern möchte.</p>
     *
     * @param req Map mit {@code gameId} und {@code roundNumber}
     */
    @MessageMapping("/game.sync")
    public void sync(@Payload Map<String, Object> req) {
        Long gameId = Long.valueOf(req.get("gameId").toString());
        int roundNumber = Integer.parseInt(req.get("roundNumber").toString());
        gameRoundOrchestrator.syncRound(gameId, roundNumber);
    }
}
