package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameMatchMessage {
    private String type = "GAME_MATCHED";
    private Long gameId;
    private PlayerDTO opponent;
    private String category;

    public GameMatchMessage(Long gameId, PlayerDTO opponent, String category) {
        this.gameId = gameId;
        this.opponent = opponent;
        this.category = category;
    }
}