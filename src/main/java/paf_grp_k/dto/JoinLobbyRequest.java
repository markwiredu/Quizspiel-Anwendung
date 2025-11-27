package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class JoinLobbyRequest {
    private Long playerId;
    private String category;



}