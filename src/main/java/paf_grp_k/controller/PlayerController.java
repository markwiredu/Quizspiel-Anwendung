package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.dto.CreatePlayerRequest;
import paf_grp_k.dto.PlayerResponse;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;

import java.util.List;

/**
 * REST-Controller zur Verwaltung von Spielern.
 *
 * <p>Diese Klasse stellt Endpunkte zur Erstellung und Abfrage von Spielern bereit.
 * Sensible Informationen wie Passwörter werden ausschließlich gehasht gespeichert
 * und niemals an den Client zurückgegeben.</p>
 */
@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    /**
     * Standard-Avatar, der verwendet wird, wenn beim Anlegen
     * eines Spielers kein Profilbild angegeben wird.
     *
     */
    private static final String DEFAULT_AVATAR_URL = "/images/default-avatar.jpg";

    /**
     * Passwort-Encoder zum sicheren Hashen von Spielerpasswörtern.
     */
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Repository zum Zugriff auf persistierte Spieler.
     */
    private final PlayerRepository playerRepository;

    /**
     * Erstellt einen neuen Spieler.
     *
     * <p>Der Endpunkt führt mehrere Validierungen durch:</p>
     * <ul>
     *     <li>Username darf nicht leer sein</li>
     *     <li>Passwort muss mindestens 8 Zeichen lang sein</li>
     *     <li>Username muss eindeutig sein</li>
     * </ul>
     *
     * <p>Passwörter werden mit BCrypt gehasht gespeichert.
     * Falls kein Profilbild angegeben wird, wird ein Standard-Avatar gesetzt.</p>
     *
     * <p>Mögliche HTTP-Antworten:</p>
     * <ul>
     *     <li>{@code 200 OK} – Spieler erfolgreich erstellt</li>
     *     <li>{@code 400 Bad Request} – ungültige Eingabedaten</li>
     * </ul>
     *
     * @param req Request-DTO mit Username, Passwort und optionalem Profilbild
     * @return {@link ResponseEntity} mit {@link PlayerResponse}
     */
    @PostMapping
    public ResponseEntity<?> createPlayer(@RequestBody CreatePlayerRequest req) {

        String username = req.getUsername() == null
                ? ""
                : req.getUsername().trim();

        String password = req.getPassword();

        // Validierung der Eingaben
        if (username.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Username darf nicht leer sein.");
        }

        if (password == null || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body("Passwort muss mindestens 8 Zeichen haben.");
        }

        if (playerRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                    .body("Username ist bereits vergeben.");
        }

        // Standard-Avatar setzen, falls keiner angegeben wurde
        String avatar =
                (req.getProfileImageUrl() == null || req.getProfileImageUrl().trim().isEmpty())
                        ? DEFAULT_AVATAR_URL
                        : req.getProfileImageUrl().trim();

        Player player = new Player();
        player.setUsername(username);
        player.setPasswordHash(passwordEncoder.encode(password));
        player.setProfileImageUrl(avatar);
        player.setTotalGames(0);
        player.setGamesWon(0);
        player.setGamesLost(0);
        player.setHighscore(0);

        return ResponseEntity.ok(
                toResponse(playerRepository.save(player))
        );
    }

    /**
     * Liefert alle registrierten Spieler.
     *
     * <p>Die Rückgabe enthält ausschließlich öffentliche Spielerdaten.</p>
     *
     * @return Liste aller Spieler als {@link PlayerResponse}
     */
    @GetMapping
    public List<PlayerResponse> getAllPlayers() {
        return playerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Liefert einen einzelnen Spieler anhand seiner ID.
     *
     * @param id eindeutige Spieler-ID
     * @return {@link PlayerResponse} oder {@code 404 Not Found}, falls nicht vorhanden
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id) {
        return playerRepository.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Wandelt ein {@link Player}-Entity in ein {@link PlayerResponse}-DTO um.
     *
     * <p>Sensible Daten wie Passwort-Hashes werden nicht übertragen.</p>
     *
     * @param player persistiertes Spieler-Entity
     * @return DTO mit öffentlichen Spielerdaten
     */
    private PlayerResponse toResponse(Player player) {
        PlayerResponse r = new PlayerResponse();
        r.setId(player.getId());
        r.setUsername(player.getUsername());
        r.setProfileImageUrl(player.getProfileImageUrl());
        r.setTotalGames(player.getTotalGames());
        r.setGamesWon(player.getGamesWon());
        r.setGamesLost(player.getGamesLost());
        r.setHighscore(player.getHighscore());
        return r;
    }
}
