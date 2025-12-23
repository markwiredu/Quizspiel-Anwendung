package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NextRoundRequest {
    private Long gameId;
    private int roundNumber;
}