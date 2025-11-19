package paf_grp_k.controller;

import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-Controller für Spieler.
 *
 * <p>Dieser Controller stellt Endpunkte bereit, um Spieler zu verwalten und
 * abzurufen. Aktuell sind nur Leseoperationen (GET) implementiert.</p>
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
     * Gibt eine Liste aller Spieler zurück.
     *
     * <p>HTTP GET auf {@code /api/players}</p>
     *
     * @return Liste aller Spieler in der Datenbank
     */
    @GetMapping
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    /**
     * Gibt einen einzelnen Spieler anhand seiner ID zurück.
     *
     * <p>HTTP GET auf {@code /api/players/{id}}</p>
     *
     * @param id die eindeutige ID des Spielers
     * @return der Spieler mit der angegebenen ID
     * @throws RuntimeException wenn kein Spieler mit dieser ID gefunden wird
     */
    @GetMapping("/{id}")
    public Player getPlayer(@PathVariable Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Player not found"));
    }
}
