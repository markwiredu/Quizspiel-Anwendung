package paf_grp_k.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import paf_grp_k.dto.GameMatchMessage;
import paf_grp_k.dto.JoinLobbyRequest;
import paf_grp_k.dto.LobbyStatusDTO;
import paf_grp_k.dto.PlayerDTO;
import paf_grp_k.model.Game;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.service.GameService;
import paf_grp_k.service.LobbyService;
import paf_grp_k.websocket.GameWebSocketNotifier;
import paf_grp_k.orchestrator.GameRoundOrchestrator;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyMatchOrchestrator {

    private final LobbyService lobbyService;
    private final GameService gameService;
    private final PlayerRepository playerRepository;
    private final GameWebSocketNotifier notifier;
    private final GameRoundOrchestrator gameRoundOrchestrator;
    private final java.util.concurrent.ConcurrentHashMap<String, Object> matchLocks =
            new java.util.concurrent.ConcurrentHashMap<>();



    public void join(JoinLobbyRequest request) {
        try {
            log.info("🎮 SPIELER {} MÖCHTE LOBBY '{}' BEITRETEN",
                    request.getPlayerId(), request.getCategory());

            // 1) Lobby Update broadcast
            broadcastLobbyUpdate(request.getCategory());

            // 2) join lobby
            LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(
                    request.getPlayerId(),
                    request.getCategory()
            );

            // 3) status an spieler
            notifier.sendLobbyStatus(request.getPlayerId(), lobbyStatus);

            // 4) Lobby Update broadcast
            broadcastLobbyUpdate(request.getCategory());

            // 5) matchmaking prüfen
            checkForMatch(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.join: {}", e.getMessage(), e);
            notifier.sendError(request.getPlayerId(), "Fehler beim Beitritt: " + e.getMessage());
        }
    }

    public void leave(JoinLobbyRequest request) {
        try {
            log.info("🚪 SPIELER {} VERLÄSST LOBBY {}",
                    request.getPlayerId(), request.getCategory());

            lobbyService.leaveLobby(request.getPlayerId(), request.getCategory());
            broadcastLobbyUpdate(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.leave: {}", e.getMessage(), e);
            notifier.sendError(request.getPlayerId(), "Fehler beim Verlassen: " + e.getMessage());
        }
    }


    // ===== interne Logik =====

    private void checkForMatch(String category) {
        String key = (category == null || category.isBlank()) ? "DEFAULT" : category.toUpperCase();
        matchLocks.putIfAbsent(key, new Object());

        synchronized (matchLocks.get(key)) {
            log.info("🔍 PRÜFE MATCH FÜR KATEGORIE: {}", category);

            Optional<LobbyService.MatchResult> matchOpt = lobbyService.checkAndCreateMatch(category);

            if (matchOpt.isEmpty()) {
                log.debug("⏳ KEIN MATCH IN LOBBY {} GEFUNDEN", category);
                return;
            }

            LobbyService.MatchResult match = matchOpt.get();

            // ✅ Hard-Block: niemals Self-Match zulassen
            if (match.player1Id != null && match.player1Id.equals(match.player2Id)) {
                log.error("❌ SELF_MATCH BLOCKED: {}", match);
                lobbyService.resetMatchmaking(match.player1Id, match.player2Id, match.category);
                broadcastLobbyUpdate(category);
                return;
            }

            log.info("✅ MATCH GEFUNDEN: {} vs {} (Kategorie: {})",
                    match.player1Id, match.player2Id, match.category);

            try {
                Game game = gameService.createGame(
                        match.player1Id,
                        match.player2Id,
                        match.category
                );

                log.info("🎮 SPIEL ERSTELLT: ID {}", game.getId());

                lobbyService.removePlayersAfterMatch(
                        match.player1Id,
                        match.player2Id,
                        match.category
                );

                notifyMatchFound(game, match.player1Id, match.player2Id);

                startGameCountdownAsync(game);

                broadcastLobbyUpdate(category);

            } catch (Exception e) {
                log.error("❌ FEHLER BEIM ERSTELLEN DES SPIELS: {}", e.getMessage(), e);

                lobbyService.resetMatchmaking(
                        match.player1Id,
                        match.player2Id,
                        match.category
                );

                notifier.sendError(match.player1Id, "Fehler beim Spielstart: " + e.getMessage());
                notifier.sendError(match.player2Id, "Fehler beim Spielstart: " + e.getMessage());

                broadcastLobbyUpdate(category);
            }
        }
    }


    private void notifyMatchFound(Game game, Long player1Id, Long player2Id) {
        Player player1 = playerRepository.findById(player1Id).orElseThrow(() ->
                new RuntimeException("Spieler " + player1Id + " nicht gefunden"));

        Player player2 = playerRepository.findById(player2Id).orElseThrow(() ->
                new RuntimeException("Spieler " + player2Id + " nicht gefunden"));

        // Spieler 1 bekommt Gegner=Spieler2
        GameMatchMessage msg1 = new GameMatchMessage(
                game.getId(),
                convertToPlayerDTO(player2),
                game.getCategory()
        );

        // Spieler 2 bekommt Gegner=Spieler1
        GameMatchMessage msg2 = new GameMatchMessage(
                game.getId(),
                convertToPlayerDTO(player1),
                game.getCategory()
        );

        notifier.sendMatchFound(player1Id, msg1);
        notifier.sendMatchFound(player2Id, msg2);

    }

    @Async
    public void startGameCountdownAsync(Game game) {
        try {
            log.info("⏱️ STARTE COUNTDOWN FÜR SPIEL {}", game.getId());

            for (int i = 5; i > 0; i--) {
                notifier.broadcastCountdown(game.getId(), i);
                Thread.sleep(1000);
            }

            log.info("🚀 SPIEL {} STARTET JETZT", game.getId());

            gameService.startGame(game.getId());

            gameRoundOrchestrator.startFirstRound(game.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Countdown unterbrochen");
        } catch (Exception e) {
            log.error("❌ Fehler beim Spielstart: {}", e.getMessage(), e);
        }
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

        notifier.broadcastLobbyUpdate(category, update);
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
