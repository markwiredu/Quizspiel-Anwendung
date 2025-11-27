package paf_grp_k.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JoinLobbyResponse {
    private String status;
    private Long gameId;
    private String message;

    public JoinLobbyResponse(String status, Long gameId, String message) {
        this.status = status;
        this.gameId = gameId;
        this.message = message;
    }
}