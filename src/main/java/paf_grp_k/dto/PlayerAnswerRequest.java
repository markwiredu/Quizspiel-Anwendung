package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class PlayerAnswerRequest {
    private Long gameId;
    private Long playerId;
    private int roundNumber;
    private String selectedAnswer;
    private long responseTime;
}