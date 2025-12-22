package paf_grp_k.service;

import paf_grp_k.dto.LobbyStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private final Map<String, Queue<Long>> lobbyQueues = new ConcurrentHashMap<>();

    /* ===================== JOIN LOBBY ===================== */
    public LobbyStatusDTO joinLobby(Long playerId, String category) {
        String lobbyCategory = normalize(category);

        lobbyQueues.putIfAbsent(lobbyCategory, new LinkedList<>());
        Queue<Long> queue = lobbyQueues.get(lobbyCategory);

        if (!queue.contains(playerId)) {
            queue.add(playerId);
        }

        int position = getPositionInQueue(playerId, queue);
        int totalPlayers = queue.size();

        return new LobbyStatusDTO(
                "WAITING",
                position,
                totalPlayers,
                "Warte auf Gegner... Position: " + position + " von " + totalPlayers
        );
    }

    /* ===================== MATCHMAKING ===================== */
    public Optional<MatchResult> checkForMatch(String category) {
        String lobbyCategory = normalize(category);
        Queue<Long> queue = lobbyQueues.get(lobbyCategory);

        if (queue != null && queue.size() >= 2) {
            Long player1Id = queue.poll();
            Long player2Id = queue.poll();

            return Optional.of(new MatchResult(player1Id, player2Id, lobbyCategory));
        }
        return Optional.empty();
    }

    /* ===================== LEAVE LOBBY ===================== */
    public void leaveLobby(Long playerId, String category) {
        String lobbyCategory = normalize(category);
        Queue<Long> queue = lobbyQueues.get(lobbyCategory);

        if (queue != null) {
            queue.remove(playerId);
            if (queue.isEmpty()) {
                lobbyQueues.remove(lobbyCategory);
            }
        }
    }

    /* ===================== LOBBY INFO (WICHTIG!) ===================== */
    public LobbyInfo getLobbyInfo(String category) {
        String lobbyCategory = normalize(category);
        Queue<Long> queue = lobbyQueues.getOrDefault(lobbyCategory, new LinkedList<>());
        return new LobbyInfo(new ArrayList<>(queue));
    }

    /* ===================== HELPER ===================== */
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

    private String normalize(String category) {
        return category != null ? category : "ALL";
    }

    /* ===================== DTOs ===================== */
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
    }
}
