package paf_grp_k.dto;

import lombok.*;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateGameRequest {

    @NotNull(message = "Player 1 ID darf nicht null sein")
    private Long player1Id;

    @NotNull(message = "Player 2 ID darf nicht null sein")
    private Long player2Id;
}