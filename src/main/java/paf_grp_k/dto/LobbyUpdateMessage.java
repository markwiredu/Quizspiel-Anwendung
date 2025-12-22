// LobbyUpdateMessage.java
package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobbyUpdateMessage {
    private String category;
    private int playersInQueue;
    private int positionInQueue;
    private String status; // PLAYER_JOINED, PLAYER_LEFT, MATCH_FOUND, QUEUE_UPDATE

    @Override
    public String toString() {
        return "LobbyUpdateMessage{" +
                "category='" + category + '\'' +
                ", playersInQueue=" + playersInQueue +
                ", positionInQueue=" + positionInQueue +
                ", status='" + status + '\'' +
                '}';
    }
}

// GameStartMessage.java (als Map verwenden, kein spezielles DTO nötig)
// GameMatchMessage.java, PlayerDTO.java, LobbyStatusDTO.java, JoinLobbyRequest.java 
// bleiben wie zuvor