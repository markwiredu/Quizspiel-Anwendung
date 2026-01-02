package paf_grp_k.service;

import paf_grp_k.dto.LobbyStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class LobbyService {

    private final Map<String, ConcurrentLinkedQueue<Long>> lobbyQueues = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> lobbyLocks = new ConcurrentHashMap<>();
    private final Set<Long> playersInAnyLobby = ConcurrentHashMap.newKeySet();
    private final Set<Long> playersInMatchmaking = ConcurrentHashMap.newKeySet();

    // Debug-Map um Spielerstatus zu verfolgen
    private final Map<Long, String> playerStatus = new ConcurrentHashMap<>();

    public LobbyStatusDTO joinLobby(Long playerId, String category) {
        String lobbyKey = normalizeCategory(category);

        log.info("🚪 Spieler {} versucht Lobby {} (key: {}) zu betreten. Aktuell in Lobby: {}",
                playerId, category, lobbyKey, playersInAnyLobby.contains(playerId));

        // Verhindere doppelten Lobby-Beitritt
        if (playersInAnyLobby.contains(playerId)) {
            log.warn("⚠️ Spieler {} ist bereits in einer Lobby", playerId);
            return LobbyStatusDTO.alreadyInLobby(category);
        }

        lobbyLocks.putIfAbsent(lobbyKey, new ReentrantLock());
        ReentrantLock lock = lobbyLocks.get(lobbyKey);

        lock.lock();
        try {
            lobbyQueues.putIfAbsent(lobbyKey, new ConcurrentLinkedQueue<>());
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);

            queue.add(playerId);
            playersInAnyLobby.add(playerId);
            playerStatus.put(playerId, "WAITING_IN_LOBBY_" + category);

            int position = getPositionInQueue(playerId, queue);
            int total = queue.size();

            log.info("✅ Spieler {} Lobby {} beigetreten. Position: {}/{}. Warteschlange: {}",
                    playerId, category, position, total, queue);

            return LobbyStatusDTO.waiting(position, total, category);
        } catch (Exception e) {
            log.error("❌ Fehler beim Beitritt für Spieler {}: {}", playerId, e.getMessage());
            return LobbyStatusDTO.error("Fehler beim Beitritt: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public Optional<MatchResult> checkAndCreateMatch(String category) {
        String lobbyKey = normalizeCategory(category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            log.debug("📭 Keine Lobby für Kategorie {}", category);
            return Optional.empty();
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);

            if (queue == null || queue.size() < 2) {
                log.debug("👥 Nicht genug Spieler in Lobby {}: {}/2", category, queue != null ? queue.size() : 0);
                return Optional.empty();
            }

            // Spieler nur ansehen, nicht entfernen
            Iterator<Long> iterator = queue.iterator();
            Long player1Id = iterator.next();
            Long player2Id = iterator.next();

            log.info("🔍 Prüfe Match für {} vs {} in Kategorie {}",
                    player1Id, player2Id, category);

            // Prüfe, ob Spieler bereits im Matchmaking
            if (playersInMatchmaking.contains(player1Id) || playersInMatchmaking.contains(player2Id)) {
                log.warn("⚠️ Spieler bereits im Matchmaking: {} oder {}", player1Id, player2Id);
                return Optional.empty();
            }

            // Markiere Spieler als "im Matchmaking"
            playersInMatchmaking.add(player1Id);
            playersInMatchmaking.add(player2Id);
            playerStatus.put(player1Id, "MATCHMAKING");
            playerStatus.put(player2Id, "MATCHMAKING");

            log.info("🎯 POTENTIELLES MATCH: {} vs {} (Kategorie: {})",
                    player1Id, player2Id, category);

            return Optional.of(new MatchResult(player1Id, player2Id, category));
        } finally {
            lock.unlock();
        }
    }

    // Methode: Spieler nach erfolgreichem Match entfernen
    public void removePlayersAfterMatch(Long player1Id, Long player2Id, String category) {
        String lobbyKey = normalizeCategory(category);

        log.info("🗑️ Entferne Spieler {} und {} aus Lobby {}", player1Id, player2Id, category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            log.warn("⚠️ Lobby {} existiert nicht beim Entfernen der Spieler", lobbyKey);
            return;
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);
            if (queue != null) {
                boolean removed1 = queue.remove(player1Id);
                boolean removed2 = queue.remove(player2Id);

                playersInAnyLobby.remove(player1Id);
                playersInAnyLobby.remove(player2Id);

                playersInMatchmaking.remove(player1Id);
                playersInMatchmaking.remove(player2Id);

                playerStatus.remove(player1Id);
                playerStatus.remove(player2Id);

                log.info("✅ Spieler entfernt: {}={}, {}={}, verbleibend in Queue: {}",
                        player1Id, removed1, player2Id, removed2, queue);

                // Lösche leere Queue
                if (queue.isEmpty()) {
                    lobbyQueues.remove(lobbyKey);
                    lobbyLocks.remove(lobbyKey);
                    log.info("🗑️ Leere Lobby {} wurde gelöscht", lobbyKey);
                }
            } else {
                log.warn("⚠️ Queue für Lobby {} ist null", lobbyKey);
            }
        } finally {
            lock.unlock();
        }
    }

    // Matchmaking zurücksetzen (falls Fehler)
    public void resetMatchmaking(Long player1Id, Long player2Id, String category) {
        String lobbyKey = normalizeCategory(category);

        log.warn("🔄 Setze Matchmaking für {} und {} zurück", player1Id, player2Id);

        if (lobbyLocks.containsKey(lobbyKey)) {
            ReentrantLock lock = lobbyLocks.get(lobbyKey);
            lock.lock();
            try {
                playersInMatchmaking.remove(player1Id);
                playersInMatchmaking.remove(player2Id);
                playerStatus.put(player1Id, "RESET_TO_WAITING");
                playerStatus.put(player2Id, "RESET_TO_WAITING");
            } finally {
                lock.unlock();
            }
        } else {
            playersInMatchmaking.remove(player1Id);
            playersInMatchmaking.remove(player2Id);
        }
    }

    public void leaveLobby(Long playerId, String category) {
        String lobbyKey = normalizeCategory(category);

        log.info("🚶 Spieler {} verlässt Lobby {}", playerId, category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            log.warn("⚠️ Lobby {} existiert nicht beim Verlassen", lobbyKey);
            return;
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);
            if (queue != null) {
                boolean removed = queue.remove(playerId);
                playersInAnyLobby.remove(playerId);
                playersInMatchmaking.remove(playerId);
                playerStatus.remove(playerId);

                log.info("✅ Spieler {} erfolgreich entfernt: {}. Verbleibend: {}",
                        playerId, removed, queue);

                if (queue.isEmpty()) {
                    lobbyQueues.remove(lobbyKey);
                    lobbyLocks.remove(lobbyKey);
                    log.info("🗑️ Leere Lobby {} gelöscht", lobbyKey);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public LobbyInfo getLobbyInfo(String category) {
        String lobbyKey = normalizeCategory(category);
        ConcurrentLinkedQueue<Long> queue = lobbyQueues.getOrDefault(lobbyKey,
                new ConcurrentLinkedQueue<>());
        List<Long> playerList = new ArrayList<>(queue);
        log.debug("📊 Lobby Info für {}: {} Spieler: {}", category, playerList.size(), playerList);
        return new LobbyInfo(playerList);
    }

    // DEBUG-Methoden
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("lobbyQueues", lobbyQueues);
        debugInfo.put("playersInAnyLobby", playersInAnyLobby);
        debugInfo.put("playersInMatchmaking", playersInMatchmaking);
        debugInfo.put("playerStatus", playerStatus);
        return debugInfo;
    }

    public String getPlayerStatus(Long playerId) {
        return playerStatus.getOrDefault(playerId, "UNKNOWN");
    }

    private int getPositionInQueue(Long playerId, Queue<Long> queue) {
        int position = 1;
        for (Long id : queue) {
            if (id.equals(playerId)) {
                return position;
            }
            position++;
        }
        return 0;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "DEFAULT";
        }
        return category.toUpperCase();
    }

    // ========== INNER CLASSES ==========

    public static class LobbyInfo {
        public final List<Long> playerIds;
        public final int playerCount;

        public LobbyInfo(List<Long> playerIds) {
            this.playerIds = playerIds;
            this.playerCount = playerIds.size();
        }
    }

    public static class MatchResult {
        public final Long player1Id;
        public final Long player2Id;
        public final String category;

        public MatchResult(Long player1Id, Long player2Id, String category) {
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.category = category;
        }

        @Override
        public String toString() {
            return "MatchResult{" +
                    "player1Id=" + player1Id +
                    ", player2Id=" + player2Id +
                    ", category='" + category + '\'' +
                    '}';
        }
    }
}