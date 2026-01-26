package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.dto.LoginRequest;
import paf_grp_k.dto.PlayerResponse;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PlayerRepository playerRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<PlayerResponse> login(@RequestBody LoginRequest req) {

        if (req.getPlayerId() == null || req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Player player = playerRepository.findById(req.getPlayerId()).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).build();
        }

        // ✅ BCrypt Check
        boolean ok = passwordEncoder.matches(req.getPassword(), player.getPasswordHash());
        if (!ok) {
            return ResponseEntity.status(401).build();
        }

        // ✅ Nur "sichere" Felder zurückgeben
        PlayerResponse res = new PlayerResponse();
        res.setId(player.getId());
        res.setUsername(player.getUsername());
        res.setProfileImageUrl(player.getProfileImageUrl());
        res.setTotalGames(player.getTotalGames());
        res.setGamesWon(player.getGamesWon());
        res.setGamesLost(player.getGamesLost());
        res.setHighscore(player.getHighscore());

        return ResponseEntity.ok(res);
    }
}
