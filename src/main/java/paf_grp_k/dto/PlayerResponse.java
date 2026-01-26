package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerResponse {
    private Long id;
    private String username;
    private String profileImageUrl;
    private int totalGames;
    private int gamesWon;
    private int gamesLost;
    private int highscore;


}