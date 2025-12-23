package paf_grp_k.service;

import paf_grp_k.dto.LobbyStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private final Map<String, ConcurrentLinkedQueue<Long>> lobbyQueues = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> lobbyLocks = new ConcurrentHashMap<>();
    private final Set<Long> playersInAnyLobby = ConcurrentHashMap.newKeySet();

    public LobbyStatusDTO joinLobby(Long playerId, String category) {
        String lobbyKey = normalizeCategory(category);

        // Verhindere doppelten Lobby-Beitritt
        if (playersInAnyLobby.contains(playerId)) {
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

            int position = getPositionInQueue(playerId, queue);

            return LobbyStatusDTO.waiting(position, queue.size(), category);
        } catch (Exception e) {
            return LobbyStatusDTO.error("Fehler beim Beitritt: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public Optional<MatchResult> checkAndCreateMatch(String category) {
        String lobbyKey = normalizeCategory(category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            return Optional.empty();
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);

            if (queue != null && queue.size() >= 2) {
                Long player1Id = queue.poll();
                Long player2Id = queue.poll();

                if (player1Id != null && player2Id != null) {
                    playersInAnyLobby.remove(player1Id);
                    playersInAnyLobby.remove(player2Id);

                    return Optional.of(new MatchResult(player1Id, player2Id, category));
                }
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public void leaveLobby(Long playerId, String category) {
        String lobbyKey = normalizeCategory(category);

        if (!lobbyLocks.containsKey(lobbyKey)) {
            return;
        }

        ReentrantLock lock = lobbyLocks.get(lobbyKey);
        lock.lock();
        try {
            ConcurrentLinkedQueue<Long> queue = lobbyQueues.get(lobbyKey);
            if (queue != null) {
                queue.remove(playerId);
                playersInAnyLobby.remove(playerId);

                if (queue.isEmpty()) {
                    lobbyQueues.remove(lobbyKey);
                    lobbyLocks.remove(lobbyKey);
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
        return new LobbyInfo(new ArrayList<>(queue));
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