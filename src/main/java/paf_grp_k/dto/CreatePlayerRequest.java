package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePlayerRequest {
    private String username;
    private String password;
    private String profileImageUrl;
}