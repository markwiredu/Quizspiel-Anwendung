package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobbyStatusDTO {
    private String status; // WAITING, MATCHED, LEFT, GAME_STARTING, ALREADY_IN_LOBBY, ERROR
    private int positionInQueue;
    private int totalPlayersInQueue;
    private Long gameId;
    private String message;
    private String category;

    // Konstruktor ohne gameId für einfachere Erstellung
    public LobbyStatusDTO(String status, int positionInQueue, int totalPlayersInQueue, String message, String category) {
        this.status = status;
        this.positionInQueue = positionInQueue;
        this.totalPlayersInQueue = totalPlayersInQueue;
        this.message = message;
        this.category = category;
    }

    // Factory-Methoden für bessere Lesbarkeit (müssen static sein!)
    public static LobbyStatusDTO waiting(int position, int total, String category) {
        return new LobbyStatusDTO(
                "WAITING",
                position,
                total,
                "Warte auf Gegner... Position: " + position + " von " + total,
                category
        );
    }

    public static LobbyStatusDTO error(String message) {
        return new LobbyStatusDTO("ERROR", 0, 0, message, null);
    }

    public static LobbyStatusDTO alreadyInLobby(String category) {
        return new LobbyStatusDTO(
                "ALREADY_IN_LOBBY",
                0, 0,
                "Du bist bereits in einer Lobby!",
                category
        );
    }

    public static LobbyStatusDTO matched(Long gameId, String category) {
        return new LobbyStatusDTO(
                "MATCHED",
                0,
                0,
                "Gegner gefunden! Spiel startet gleich...",
                category
        );
    }
}