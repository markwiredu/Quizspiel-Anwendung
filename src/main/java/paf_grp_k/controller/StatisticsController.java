package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST-Controller zur Bereitstellung statistischer Auswertungen.
 *
 * <p>Diese Klasse stellt Endpunkte zur Verfügung, die aggregierte
 * Spielerstatistiken liefern, z. B. Ranglisten oder Vergleichswerte.</p>
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    /**
     * Repository zum Zugriff auf persistierte Spieler- und Statistikdaten.
     */
    private final PlayerRepository playerRepository;

    /**
     * Liefert eine Rangliste aller Spieler basierend auf ihren Spielergebnissen.
     *
     * <p>Die Sortierung erfolgt nach folgenden Kriterien:</p>
     * <ol>
     *     <li>Anzahl gewonnener Spiele (absteigend)</li>
     *     <li>Gewinnrate (absteigend) bei gleicher Anzahl an Siegen</li>
     * </ol>
     *
     * <p>Es werden ausschließlich Spieler berücksichtigt, die mindestens
     * ein Spiel absolviert haben.</p>
     *
     * <p>Die Gewinnrate wird in Prozent berechnet und auf eine Nachkommastelle
     * gerundet.</p>
     *
     * @return {@link ResponseEntity} mit einer Liste von Ranglisten-Einträgen
     *         als {@code Map<String, Object>}
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {

        List<Player> players = playerRepository.findAll();

        // Spieler mit mindestens einem Spiel filtern und sortieren
        List<Player> sorted = players.stream()
                .filter(player -> player.getTotalGames() > 0)
                .sorted((a, b) -> {
                    int winComparison =
                            Integer.compare(b.getGamesWon(), a.getGamesWon());

                    if (winComparison != 0) {
                        return winComparison;
                    }

                    double winRateA =
                            (double) a.getGamesWon() / a.getTotalGames();

                    double winRateB =
                            (double) b.getGamesWon() / b.getTotalGames();

                    return Double.compare(winRateB, winRateA);
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> leaderboard = new ArrayList<>();

        // Rangliste aufbauen
        for (int i = 0; i < sorted.size(); i++) {
            Player player = sorted.get(i);

            double winRate =
                    (double) player.getGamesWon()
                            / player.getTotalGames() * 100;

            Map<String, Object> entry = new HashMap<>();
            entry.put("rank", i + 1);
            entry.put("playerId", player.getId());
            entry.put("username", player.getUsername());
            entry.put("profileImageUrl", player.getProfileImageUrl());
            entry.put("gamesWon", player.getGamesWon());
            entry.put("totalGames", player.getTotalGames());
            entry.put("highscore", player.getHighscore());
            entry.put("winRate",
                    Math.round(winRate * 10) / 10.0);

            leaderboard.add(entry);
        }

        return ResponseEntity.ok(leaderboard);
    }
}
