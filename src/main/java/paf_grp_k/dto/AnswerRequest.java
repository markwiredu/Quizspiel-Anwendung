package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerRequest {
    private Long gameId;
    private Long playerId;
    private int roundNumber;
    private String answer;
}