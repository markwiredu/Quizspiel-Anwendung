package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerDTO {
    private Long id;
    private String username;
    private String profileImageUrl;
    private int totalGames;
    private int gamesWon;
    private int gamesLost;
    private int highscore;

    // Default Konstruktor für Frameworks
    public PlayerDTO() {
    }

    // Convenience Konstruktor
    public PlayerDTO(Long id, String username, String profileImageUrl) {
        this.id = id;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
    }
}