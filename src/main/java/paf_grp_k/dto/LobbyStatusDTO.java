package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LobbyStatusDTO {
    private String status; // WAITING, MATCHED, LEFT, GAME_STARTING
    private int positionInQueue;
    private int totalPlayersInQueue;
    private Long gameId;
    private String message;

    public LobbyStatusDTO(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public LobbyStatusDTO(String status, int position, int totalPlayers, String message) {
        this.status = status;
        this.positionInQueue = position;
        this.totalPlayersInQueue = totalPlayers;
        this.message = message;
    }
}