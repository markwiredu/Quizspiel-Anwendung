package paf_grp_k.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private Long playerId;
    private String password;
}
