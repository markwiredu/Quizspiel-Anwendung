package paf_grp_k.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.model.Player;
import paf_grp_k.repository.PlayerRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final PlayerRepository playerRepository;

    /**
     * Spielerstatistik abrufen
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<Map<String, Object>> getPlayerStats(@PathVariable Long playerId) {
        log.info("📊 Hole Statistiken für Spieler {}", playerId);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Spieler nicht gefunden"));

        Map<String, Object> stats = new HashMap<>();
        stats.put("playerId", player.getId());
        stats.put("username", player.getUsername());
        stats.put("profileImageUrl", player.getProfileImageUrl());
        stats.put("totalGames", player.getTotalGames());
        stats.put("gamesWon", player.getGamesWon());
        stats.put("gamesLost", player.getGamesLost());

        // Gewinnrate berechnen
        double winRate = player.getTotalGames() > 0 ?
                (double) player.getGamesWon() / player.getTotalGames() * 100 : 0;
        stats.put("winRate", Math.round(winRate * 10) / 10.0); // 1 Nachkommastelle

        return ResponseEntity.ok(stats);
    }

    /**
     * Rangliste basierend auf Siegen
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard() {
        log.info("🏆 Hole Rangliste");

        List<Player> players = playerRepository.findAll();

        // Spieler filtern und sortieren
        List<Player> sortedPlayers = players.stream()
                .filter(p -> p.getTotalGames() > 0)
                .sorted((p1, p2) -> {
                    // 1. Sortieren nach Siegen (absteigend)
                    int winsCompare = Integer.compare(p2.getGamesWon(), p1.getGamesWon());
                    if (winsCompare != 0) return winsCompare;

                    // 2. Bei gleichen Siegen: nach Gewinnrate
                    double winRate1 = p1.getTotalGames() > 0 ?
                            (double) p1.getGamesWon() / p1.getTotalGames() : 0;
                    double winRate2 = p2.getTotalGames() > 0 ?
                            (double) p2.getGamesWon() / p2.getTotalGames() : 0;
                    return Double.compare(winRate2, winRate1);
                })
                .collect(Collectors.toList());

        // Rangliste mit Rängen erstellen
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        for (int i = 0; i < sortedPlayers.size(); i++) {
            Player player = sortedPlayers.get(i);
            Map<String, Object> entry = new HashMap<>();

            entry.put("rank", i + 1);
            entry.put("playerId", player.getId());
            entry.put("username", player.getUsername());
            entry.put("profileImageUrl", player.getProfileImageUrl());
            entry.put("totalGames", player.getTotalGames());
            entry.put("gamesWon", player.getGamesWon());
            entry.put("gamesLost", player.getGamesLost());

            // Gewinnrate berechnen
            double winRate = player.getTotalGames() > 0 ?
                    (double) player.getGamesWon() / player.getTotalGames() * 100 : 0;
            entry.put("winRate", Math.round(winRate * 10) / 10.0);

            leaderboard.add(entry);
        }

        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Top 10 Spieler
     */
    @GetMapping("/leaderboard/top10")
    public ResponseEntity<List<Map<String, Object>>> getTop10() {
        log.info("🏆 Hole Top 10 Rangliste");

        List<Map<String, Object>> leaderboard = getLeaderboard().getBody();
        if (leaderboard == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // Auf Top 10 begrenzen
        List<Map<String, Object>> top10 = leaderboard.stream()
                .limit(10)
                .collect(Collectors.toList());

        return ResponseEntity.ok(top10);
    }

    /**
     * Alternative Methode mit IntStream
     */
    @GetMapping("/leaderboard2")
    public ResponseEntity<List<Map<String, Object>>> getLeaderboard2() {
        log.info("🏆 Hole Rangliste (Methode 2)");

        List<Player> players = playerRepository.findAll();

        // Spieler filtern und sortieren
        List<Player> sortedPlayers = players.stream()
                .filter(p -> p.getTotalGames() > 0)
                .sorted((p1, p2) -> {
                    // 1. Sortieren nach Siegen (absteigend)
                    int winsCompare = Integer.compare(p2.getGamesWon(), p1.getGamesWon());
                    if (winsCompare != 0) return winsCompare;

                    // 2. Bei gleichen Siegen: nach Gewinnrate
                    double winRate1 = p1.getTotalGames() > 0 ?
                            (double) p1.getGamesWon() / p1.getTotalGames() : 0;
                    double winRate2 = p2.getTotalGames() > 0 ?
                            (double) p2.getGamesWon() / p2.getTotalGames() : 0;
                    return Double.compare(winRate2, winRate1);
                })
                .collect(Collectors.toList());

        // Mit IntStream und forEach arbeiten
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        IntStream.range(0, sortedPlayers.size()).forEach(i -> {
            Player player = sortedPlayers.get(i);
            Map<String, Object> entry = new HashMap<>();

            entry.put("rank", i + 1);
            entry.put("playerId", player.getId());
            entry.put("username", player.getUsername());
            entry.put("profileImageUrl", player.getProfileImageUrl());
            entry.put("totalGames", player.getTotalGames());
            entry.put("gamesWon", player.getGamesWon());
            entry.put("gamesLost", player.getGamesLost());

            // Gewinnrate berechnen
            double winRate = player.getTotalGames() > 0 ?
                    (double) player.getGamesWon() / player.getTotalGames() * 100 : 0;
            entry.put("winRate", Math.round(winRate * 10) / 10.0);

            leaderboard.add(entry);
        });

        return ResponseEntity.ok(leaderboard);
    }

    /**
     * Spieler-Statistik aktualisieren (für Testzwecke)
     */
    @PostMapping("/player/{playerId}/update")
    public ResponseEntity<String> updatePlayerStats(
            @PathVariable Long playerId,
            @RequestParam int gamesWon,
            @RequestParam int gamesLost) {

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Spieler nicht gefunden"));

        player.setGamesWon(gamesWon);
        player.setGamesLost(gamesLost);
        player.setTotalGames(gamesWon + gamesLost);

        playerRepository.save(player);

        return ResponseEntity.ok("Statistik für " + player.getUsername() + " aktualisiert");
    }

    /**
     * Alle Spieler und ihre Statistiken anzeigen (für Debugging)
     */
    @GetMapping("/debug/players")
    public ResponseEntity<List<Map<String, Object>>> getAllPlayers() {
        List<Player> players = playerRepository.findAll();

        List<Map<String, Object>> result = players.stream().map(player -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", player.getId());
            map.put("username", player.getUsername());
            map.put("totalGames", player.getTotalGames());
            map.put("gamesWon", player.getGamesWon());
            map.put("gamesLost", player.getGamesLost());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}