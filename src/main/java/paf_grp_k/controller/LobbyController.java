package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.dto.LobbyStatusDTO;
import paf_grp_k.service.GameService;
import paf_grp_k.service.LobbyService;

@RestController
@RequestMapping("/lobby")
@RequiredArgsConstructor
public class LobbyController {

    private final LobbyService lobbyService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping("/join")
    public LobbyStatusDTO joinLobby(@RequestParam Long playerId, @RequestParam String category) {
        // Spieler in Lobby eintragen
        LobbyStatusDTO status = lobbyService.joinLobby(playerId, category);

        // Prüfe sofort auf Match
        lobbyService.checkForMatch(category).ifPresent(match -> {
            // Spiel erstellen
            var game = gameService.createGame(match.player1Id, match.player2Id, match.category);

            // Spieler per WebSocket benachrichtigen
            messagingTemplate.convertAndSendToUser(match.player1Id.toString(), "/queue/match", game);
            messagingTemplate.convertAndSendToUser(match.player2Id.toString(), "/queue/match", game);
        });

        return status;
    }

    @PostMapping("/leave")
    public void leaveLobby(@RequestParam Long playerId, @RequestParam String category) {
        lobbyService.leaveLobby(playerId, category);
    }

}
