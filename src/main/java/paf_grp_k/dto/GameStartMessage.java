package paf_grp_k.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameStartMessage {
    private String type = "GAME_START";
    private Long gameId;
    private Long opponentId;
    private String opponentUsername;

    public GameStartMessage(Long gameId, Long opponentId, String opponentUsername) {
        this.gameId = gameId;
        this.opponentId = opponentId;
        this.opponentUsername = opponentUsername;
    }
}