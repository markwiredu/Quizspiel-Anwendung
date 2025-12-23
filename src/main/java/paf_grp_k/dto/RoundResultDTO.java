package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundResultDTO {
    private Long gameId;
    private int roundNumber;
    private String correctAnswer;
    private int player1Points;
    private int player2Points;
    private String message;
    private boolean isGameFinished;
}