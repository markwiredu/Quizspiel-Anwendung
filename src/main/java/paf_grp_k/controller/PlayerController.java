package paf_grp_k.controller;

import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Controller zur Verwaltung von Spielern.
 *
 * <p>Dieser Controller stellt Endpunkte zum Abrufen, Erstellen und Abfragen von Spielern
 * bereit.</p>
 */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    /**
     * Repository für den Zugriff auf {@link Player}-Daten.
     */
    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Gibt alle Spieler zurück.
     *
     * <p>HTTP GET: {@code /api/players}</p>
     *
     * @return Liste aller Spieler
     */
    @GetMapping
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    /**
     * Gibt einen bestimmten Spieler anhand seiner ID zurück.
     *
     * <p>HTTP GET: {@code /api/players/{id}}</p>
     *
     * @param id die eindeutige ID des Spielers
     * @return der Spieler mit der angegebenen ID
     * @throws RuntimeException wenn kein Spieler mit dieser ID existiert
     */
    @GetMapping("/{id}")
    public Player getPlayerById(@PathVariable Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found with id: " + id));
    }

    /**
     * Erstellt einen neuen Spieler.
     *
     * <p>HTTP POST: {@code /api/players}</p>
     * <p>Es wird überprüft, dass Benutzername und Passwort-Hash gesetzt sind.</p>
     *
     * @param player das Spieler-Objekt, das erstellt werden soll
     * @return der gespeicherte Spieler
     * @throws RuntimeException wenn Benutzername oder Passwort-Hash fehlen
     */
    @PostMapping
    public Player createPlayer(@RequestBody Player player) {
        // Einfache Validierung
        if (player.getUsername() == null || player.getUsername().trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (player.getPasswordHash() == null || player.getPasswordHash().trim().isEmpty()) {
            throw new RuntimeException("Password hash is required");
        }

        return playerRepository.save(player);
    }
}
