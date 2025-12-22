package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStartMessage {
    private Long gameId;
    private Long player1Id;
    private Long player2Id;
    private String category;
}