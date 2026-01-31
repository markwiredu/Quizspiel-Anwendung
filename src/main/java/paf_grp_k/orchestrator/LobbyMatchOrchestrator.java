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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrator für Lobby- und Matchmaking-Abläufe.
 *
 * <p>Diese Klasse koordiniert den Prozess, mit dem Spieler einer Lobby beitreten,
 * sie verlassen und bei ausreichender Anzahl automatisch zu einem Spiel gematcht werden.</p>
 *
 * <p>Aufgaben im Überblick:</p>
 * <ul>
 *   <li>Beitritt/Verlassen einer Lobby (Delegation an {@link LobbyService})</li>
 *   <li>Matchmaking pro Kategorie inkl. Synchronisation (Lock pro Kategorie)</li>
 *   <li>Erstellung eines Spiels über {@link GameService}</li>
 *   <li>Benachrichtigung der Clients über WebSockets ({@link GameWebSocketNotifier})</li>
 *   <li>Countdown vor Spielstart und Start der ersten Runde via {@link GameRoundOrchestrator}</li>
 * </ul>
 *
 * <p>Thread-Safety: Match-Erstellung wird pro Kategorie in einem {@code synchronized}-Block
 * geschützt, um doppelte Spielstarts (Race Conditions) zu verhindern.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyMatchOrchestrator {

    /**
     * Dauer des Countdowns in Sekunden, bevor das Spiel tatsächlich startet.
     *
     */
    private static final int COUNTDOWN_SECONDS = 5;

    /**
     * Service für Lobby-Verwaltung (Queue, Matchprüfung, Lobby-Infos).
     */
    private final LobbyService lobbyService;

    /**
     * Service für Spiele (Erstellung, Start, Persistenz).
     */
    private final GameService gameService;

    /**
     * Repository zum Laden von Spielerinformationen (z. B. für Gegner-Daten).
     */
    private final PlayerRepository playerRepository;

    /**
     * Notifier für WebSocket-Kommunikation (Status, Updates, Matchfound, Countdown).
     */
    private final GameWebSocketNotifier notifier;

    /**
     * Orchestrator für den Start und Ablauf von Spielrunden.
     */
    private final GameRoundOrchestrator gameRoundOrchestrator;

    /**
     * Locks pro Kategorie, um Matchmaking und Spielstart pro Lobby zu serialisieren.
     *
     * <p>Key: normalisierte Kategorie (siehe {@link #lockKey(String)}).</p>
     */
    private final ConcurrentHashMap<String, Object> matchLocks = new ConcurrentHashMap<>();

    /**
     * Verarbeitet den Beitritt eines Spielers zu einer Lobby.
     *
     * <p>Ablauf:</p>
     * <ol>
     *   <li>Broadcast eines Lobby-Updates</li>
     *   <li>Beitritt zur Lobby über {@link LobbyService}</li>
     *   <li>Rückmeldung (LobbyStatus) an den Spieler</li>
     *   <li>Broadcast eines weiteren Lobby-Updates</li>
     *   <li>Prüfung, ob ein Match gebildet werden kann</li>
     * </ol>
     *
     * @param request Join-Request mit Spieler-ID und Kategorie
     */
    public void join(JoinLobbyRequest request) {
        try {
            log.info("🎮 SPIELER {} MÖCHTE LOBBY '{}' BEITRETEN", request.getPlayerId(), request.getCategory());

            broadcastLobbyUpdate(request.getCategory());

            LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(request.getPlayerId(), request.getCategory());
            notifier.sendLobbyStatus(request.getPlayerId(), lobbyStatus);

            broadcastLobbyUpdate(request.getCategory());
            checkForMatch(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.join: {}", e.getMessage(), e);
            notifier.sendError(request.getPlayerId(), "Fehler beim Beitritt: " + e.getMessage());
        }
    }

    /**
     * Verarbeitet das Verlassen einer Lobby durch einen Spieler.
     *
     * <p>Der Spieler wird aus der Lobby entfernt und anschließend wird ein
     * aktualisiertes Lobby-Update an Clients gebroadcastet.</p>
     *
     * @param request Leave-Request mit Spieler-ID und Kategorie
     */
    public void leave(JoinLobbyRequest request) {
        try {
            log.info("🚪 SPIELER {} VERLÄSST LOBBY {}", request.getPlayerId(), request.getCategory());

            lobbyService.leaveLobby(request.getPlayerId(), request.getCategory());
            broadcastLobbyUpdate(request.getCategory());

        } catch (Exception e) {
            log.error("❌ Fehler bei game.leave: {}", e.getMessage(), e);
            notifier.sendError(request.getPlayerId(), "Fehler beim Verlassen: " + e.getMessage());
        }
    }

    // ===== interne Logik =====

    /**
     * Normalisiert eine Kategorie zu einem stabilen Lock-Key.
     *
     * <p>Leere oder {@code null} Kategorien werden auf {@code "DEFAULT"} gemappt.</p>
     *
     * @param category Kategorie-String (kann {@code null} sein)
     * @return normalisierter Lock-Key
     */
    private String lockKey(String category) {
        return (category == null || category.isBlank()) ? "DEFAULT" : category.toUpperCase();
    }

    /**
     * Prüft, ob in einer Kategorie ein Match gebildet werden kann, und startet bei Erfolg ein Spiel.
     *
     * <p>Diese Methode ist pro Kategorie synchronisiert, damit zwei parallele Join-Requests
     * nicht gleichzeitig dasselbe Match erzeugen können.</p>
     *
     * <p>Ablauf bei Match:</p>
     * <ol>
     *   <li>Match über {@link LobbyService#checkAndCreateMatch(String)} ermitteln</li>
     *   <li>Self-Match verhindern (Hard-Block)</li>
     *   <li>Spiel erzeugen via {@link GameService#createGame(Long, Long, String)}</li>
     *   <li>Spieler aus der Lobby entfernen</li>
     *   <li>Clients über Match informieren</li>
     *   <li>Countdown starten und Spiel beginnen</li>
     * </ol>
     *
     * @param category Kategorie, in der geprüft wird
     */
    private void checkForMatch(String category) {
        String key = lockKey(category);
        Object lock = matchLocks.computeIfAbsent(key, k -> new Object());

        synchronized (lock) {
            log.info("🔍 PRÜFE MATCH FÜR KATEGORIE: {}", category);

            var matchOpt = lobbyService.checkAndCreateMatch(category);
            if (matchOpt.isEmpty()) {
                log.debug("⏳ KEIN MATCH IN LOBBY {} GEFUNDEN", category);
                return;
            }

            LobbyService.MatchResult match = matchOpt.get();

            // Hard-Block: niemals Self-Match zulassen
            if (match.player1Id != null && match.player1Id.equals(match.player2Id)) {
                log.error("❌ SELF_MATCH BLOCKED: {}", match);
                lobbyService.resetMatchmaking(match.player1Id, match.player2Id, match.category);
                broadcastLobbyUpdate(category);
                return;
            }

            log.info("✅ MATCH GEFUNDEN: {} vs {} (Kategorie: {})", match.player1Id, match.player2Id, match.category);

            try {
                Game game = gameService.createGame(match.player1Id, match.player2Id, match.category);
                log.info("🎮 SPIEL ERSTELLT: ID {}", game.getId());

                lobbyService.removePlayersAfterMatch(match.player1Id, match.player2Id, match.category);

                notifyMatchFound(game, match.player1Id, match.player2Id);
                startGameCountdownAsync(game);

                broadcastLobbyUpdate(category);

            } catch (Exception e) {
                log.error("❌ FEHLER BEIM ERSTELLEN DES SPIELS: {}", e.getMessage(), e);

                lobbyService.resetMatchmaking(match.player1Id, match.player2Id, match.category);

                notifier.sendError(match.player1Id, "Fehler beim Spielstart: " + e.getMessage());
                notifier.sendError(match.player2Id, "Fehler beim Spielstart: " + e.getMessage());

                broadcastLobbyUpdate(category);
            }
        }
    }

    /**
     * Lädt einen Spieler anhand seiner ID oder wirft eine Exception, wenn er nicht existiert.
     *
     * @param id Spieler-ID
     * @return persistierter {@link Player}
     * @throws RuntimeException wenn kein Spieler gefunden wird
     */
    private Player requirePlayer(Long id) {
        return playerRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Spieler " + id + " nicht gefunden"));
    }

    /**
     * Informiert beide Spieler über ein gefundenes Match.
     *
     * <p>Jeder Spieler erhält die Spiel-ID sowie ein {@link PlayerDTO} des Gegners.</p>
     *
     * @param game neu erstelltes Spiel
     * @param player1Id ID von Spieler 1
     * @param player2Id ID von Spieler 2
     */
    private void notifyMatchFound(Game game, Long player1Id, Long player2Id) {
        Player player1 = requirePlayer(player1Id);
        Player player2 = requirePlayer(player2Id);

        notifier.sendMatchFound(player1Id,
                new GameMatchMessage(game.getId(), convertToPlayerDTO(player2), game.getCategory()));
        notifier.sendMatchFound(player2Id,
                new GameMatchMessage(game.getId(), convertToPlayerDTO(player1), game.getCategory()));
    }

    /**
     * Startet den Spielstart-Countdown asynchron und beginnt anschließend das Spiel.
     *
     * <p>Während des Countdowns wird pro Sekunde ein Update an alle Clients des Spiels gebroadcastet.
     * Nach Ablauf wird das Spiel über {@link GameService#startGame(Long)} gestartet und die erste Runde
     * über {@link GameRoundOrchestrator#startFirstRound(Long)} initialisiert.</p>
     *
     * <p>Hinweis: Diese Methode ist {@code @Async}. Daher läuft sie in einem separaten Thread.</p>
     *
     * @param game Spiel, das gestartet werden soll
     */
    @Async
    public void startGameCountdownAsync(Game game) {
        try {
            log.info("⏱️ STARTE COUNTDOWN FÜR SPIEL {}", game.getId());

            for (int i = COUNTDOWN_SECONDS; i > 0; i--) {
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

    /**
     * Broadcastet den aktuellen Zustand der Lobby (pro Kategorie) an Clients.
     *
     * <p>Enthält u. a. Spieleranzahl, Spieler-IDs sowie einen Timestamp.
     * Der Payload ist als {@code Map} strukturiert und enthält den Typ {@code LOBBY_UPDATE}.</p>
     *
     * @param category Kategorie der Lobby
     */
    private void broadcastLobbyUpdate(String category) {
        LobbyService.LobbyInfo lobbyInfo = lobbyService.getLobbyInfo(category);

        notifier.broadcastLobbyUpdate(category, Map.of(
                "type", "LOBBY_UPDATE",
                "category", category,
                "playerCount", lobbyInfo.playerCount,
                "playerIds", lobbyInfo.playerIds,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * Konvertiert ein {@link Player}-Entity in ein {@link PlayerDTO}.
     *
     * <p>Das DTO enthält ausschließlich öffentliche Informationen und kann
     * sicher über WebSockets an Clients übertragen werden.</p>
     *
     * @param player Spieler-Entity
     * @return befülltes {@link PlayerDTO}
     */
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
