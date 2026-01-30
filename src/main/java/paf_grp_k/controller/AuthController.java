package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.dto.LoginRequest;
import paf_grp_k.dto.PlayerResponse;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;

/**
 * REST-Controller für Authentifizierungsfunktionen.
 *
 * <p>Diese Klasse stellt Endpunkte zur Anmeldung von Spielern bereit.
 * Die Authentifizierung erfolgt über eine Spieler-ID und ein Passwort,
 * welches mit einem gespeicherten BCrypt-Hash verglichen wird.</p>
 *
 * <p>Der Controller gibt bei erfolgreicher Anmeldung ausschließlich
 * nicht-sensible Spielerdaten zurück.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * Repository zum Zugriff auf persistierte Spieler.
     */
    private final PlayerRepository playerRepository;

    /**
     * Passwort-Encoder zum sicheren Vergleich von Klartext-Passwörtern
     * mit gespeicherten BCrypt-Hashes.
     */
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Authentifiziert einen Spieler anhand seiner Zugangsdaten.
     *
     * <p>Der Endpunkt überprüft:</p>
     * <ul>
     *     <li>ob die übergebenen Login-Daten vollständig sind</li>
     *     <li>ob ein Spieler mit der angegebenen ID existiert</li>
     *     <li>ob das Passwort mit dem gespeicherten Hash übereinstimmt</li>
     * </ul>
     *
     * <p>Mögliche HTTP-Antworten:</p>
     * <ul>
     *     <li>{@code 200 OK} – Anmeldung erfolgreich, Spielerdaten werden zurückgegeben</li>
     *     <li>{@code 400 Bad Request} – ungültige oder unvollständige Eingabedaten</li>
     *     <li>{@code 401 Unauthorized} – Spieler existiert nicht oder Passwort falsch</li>
     * </ul>
     *
     * @param req Login-Daten bestehend aus Spieler-ID und Passwort
     * @return {@link ResponseEntity} mit {@link PlayerResponse} bei Erfolg
     */
    @PostMapping("/login")
    public ResponseEntity<PlayerResponse> login(@RequestBody LoginRequest req) {

        // Validierung der Eingabedaten
        if (req.getPlayerId() == null || req.getPassword() == null || req.getPassword().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return playerRepository.findById(req.getPlayerId())
                .filter(player ->
                        passwordEncoder.matches(req.getPassword(), player.getPasswordHash()))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    /**
     * Wandelt ein {@link Player}-Entity in ein {@link PlayerResponse}-DTO um.
     *
     * <p>Es werden ausschließlich öffentlich relevante Informationen übertragen.
     * Sensible Daten wie Passwort-Hashes werden bewusst nicht weitergegeben.</p>
     *
     * @param player persistiertes Spieler-Entity
     * @return DTO mit öffentlichen Spielerdaten
     */
    private PlayerResponse toResponse(Player player) {
        PlayerResponse res = new PlayerResponse();
        res.setId(player.getId());
        res.setUsername(player.getUsername());
        res.setProfileImageUrl(player.getProfileImageUrl());
        res.setTotalGames(player.getTotalGames());
        res.setGamesWon(player.getGamesWon());
        res.setGamesLost(player.getGamesLost());
        res.setHighscore(player.getHighscore());
        return res;
    }
}
