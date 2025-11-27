package paf_grp_k.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinishGameRequest {
    private int scorePlayer1;
    private int scorePlayer2;
    private Long winnerId;
}