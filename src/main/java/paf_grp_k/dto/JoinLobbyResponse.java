package paf_grp_k.dto;

public class JoinLobbyResponse {
    private String status;
    private Long gameId;
    private String message;

    public JoinLobbyResponse(String status, Long gameId, String message) {
        this.status = status;
        this.gameId = gameId;
        this.message = message;
    }

    // Getter und Setter
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
