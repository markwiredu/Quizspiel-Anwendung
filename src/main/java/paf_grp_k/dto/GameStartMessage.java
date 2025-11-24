package paf_grp_k.dto;

public class GameStartMessage {
    private String type = "GAME_START";
    private Long gameId;
    private Long opponentId;
    private String opponentUsername;

    public GameStartMessage(Long gameId, Long opponentId, String opponentUsername) {
        this.gameId = gameId;
        this.opponentId = opponentId;
        this.opponentUsername = opponentUsername;
    }

    // Getter und Setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public Long getOpponentId() { return opponentId; }
    public void setOpponentId(Long opponentId) { this.opponentId = opponentId; }

    public String getOpponentUsername() { return opponentUsername; }
    public void setOpponentUsername(String opponentUsername) { this.opponentUsername = opponentUsername; }
}
