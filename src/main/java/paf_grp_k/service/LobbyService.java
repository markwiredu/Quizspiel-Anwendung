package paf_grp_k.service;

import paf_grp_k.dto.LobbyStatusDTO;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LobbyService {

    private final PlayerRepository playerRepository;
    private final Map<String, Queue<Long>> lobbyQueues = new ConcurrentHashMap<>();

    /**
     * Spieler zur Lobby hinzufügen
     */
    public LobbyStatusDTO joinLobby(Long playerId, String category) { // Rückgabetyp geändert
        String lobbyCategory = category != null ? category : "ALL";

        lobbyQueues.putIfAbsent(lobbyCategory, new LinkedList<>());
        Queue<Long> queue = lobbyQueues.get(lobbyCategory);

        if (!queue.contains(playerId)) {
            queue.add(playerId);
        }

        int position = getPositionInQueue(playerId, lobbyCategory);
        int totalPlayers = queue.size();

        return new LobbyStatusDTO(
                "WAITING",
                position,
                totalPlayers,
                "Warte auf Gegner... Position: " + position + " von " + totalPlayers
        );
    }

    private int getPositionInQueue(Long playerId, String category) {
        Queue<Long> queue = lobbyQueues.get(category);
        if (queue == null) return 0;

        int position = 1;
        for (Long id : queue) {
            if (id.equals(playerId)) {
                return position;
            }
            position++;
        }
        return 0;
    }

    public Optional<MatchResult> checkForMatch(String category) {
        Queue<Long> queue = lobbyQueues.get(category);
        if (queue != null && queue.size() >= 2) {
            Long player1Id = queue.poll();
            Long player2Id = queue.poll();

            return Optional.of(new MatchResult(player1Id, player2Id, category));
        }
        return Optional.empty();
    }

    public void leaveLobby(Long playerId, String category) {
        String lobbyCategory = category != null ? category : "ALL";
        Queue<Long> queue = lobbyQueues.get(lobbyCategory);
        if (queue != null) {
            queue.remove(playerId);
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