package paf_grp_k.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import paf_grp_k.dto.CreatePlayerRequest;
import paf_grp_k.dto.PlayerResponse;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-Controller zur Verwaltung von Spielern.
 *
 * <p>Dieser Controller stellt Endpunkte bereit, um Spieler zu registrieren,
 * Informationen über alle Spieler abzurufen sowie einen einzelnen Spieler
 * anhand seiner ID zu erhalten.</p>
 */
@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {

    // BCryptPasswordEncoder wird von Spring automatisch bereitgestellt
    private final BCryptPasswordEncoder passwordEncoder;

    private final PlayerRepository playerRepository;

    /**
     * Registriert einen neuen Spieler.
     *
     * <p>Es wird geprüft, ob der Benutzername bereits vergeben ist. Falls ja,
     * wird ein {@code 400 Bad Request} zurückgegeben. Andernfalls wird ein neuer
     * Spieler angelegt, gespeichert und als {@link PlayerResponse} zurückgegeben.</p>
     *
     * @param request Daten des anzulegenden Spielers
     * @return HTTP-200 mit Spielerinformationen oder HTTP-400 bei Fehlern
     */
    @PostMapping
    public ResponseEntity<PlayerResponse> createPlayer(@RequestBody CreatePlayerRequest request) {
        // Prüfen ob Username bereits existiert
        if (playerRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        // Neuen Spieler erstellen
        Player player = new Player();
        player.setUsername(request.getUsername());
        /*player.setPasswordHash(request.getPassword()); // In echten Applikationen hashen!
        Ersetzen durch:
         */
        // Passwort wird hier sicher gehasht gespeichert (kein Klartext!)
        player.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        player.setProfileImageUrl(request.getProfileImageUrl());
        player.setTotalGames(0);
        player.setGamesWon(0);
        player.setGamesLost(0);
        player.setHighscore(0);

        Player savedPlayer = playerRepository.save(player);

        // Response erstellen
        PlayerResponse response = new PlayerResponse();
        response.setId(savedPlayer.getId());
        response.setUsername(savedPlayer.getUsername());
        response.setProfileImageUrl(savedPlayer.getProfileImageUrl());
        response.setTotalGames(savedPlayer.getTotalGames());
        response.setGamesWon(savedPlayer.getGamesWon());
        response.setGamesLost(savedPlayer.getGamesLost());
        response.setHighscore(savedPlayer.getHighscore());

        return ResponseEntity.ok(response);
    }

    /**
     * Ruft alle Spieler aus der Datenbank ab.
     *
     * <p>Die Spieler werden in eine Liste von {@link PlayerResponse} Objekten
     * konvertiert, um nur relevante Informationen zurückzugeben.</p>
     *
     * @return Liste aller Spieler als {@link PlayerResponse}
     */
    @GetMapping
    public List<PlayerResponse> getAllPlayers() {
        return playerRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Ruft einen einzelnen Spieler anhand seiner ID ab.
     *
     * <p>Falls der Spieler nicht existiert, wird ein {@code 404 Not Found}
     * zurückgegeben.</p>
     *
     * @param id ID des gewünschten Spielers
     * @return HTTP-200 mit Spielerinformationen oder HTTP-404 wenn nicht gefunden
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlayerResponse> getPlayerById(@PathVariable Long id) {
        return playerRepository.findById(id)
                .map(this::convertToResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Konvertiert ein {@link Player}-Entity in ein {@link PlayerResponse}-DTO.
     *
     * <p>Diese Hilfsmethode stellt sicher, dass nur benötigte Daten an das
     * Frontend zurückgegeben werden.</p>
     *
     * @param player Spieler-Entity aus der Datenbank
     * @return Konvertierte {@link PlayerResponse}-Darstellung
     */
    private PlayerResponse convertToResponse(Player player) {
        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setUsername(player.getUsername());
        response.setProfileImageUrl(player.getProfileImageUrl());
        response.setTotalGames(player.getTotalGames());
        response.setGamesWon(player.getGamesWon());
        response.setGamesLost(player.getGamesLost());
        response.setHighscore(player.getHighscore());
        return response;
    }
}
