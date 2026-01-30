package paf_grp_k.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paf_grp_k.model.*;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import paf_grp_k.repository.RoundRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service-Schicht für Spiel- und Rundenlogik.
 *
 * <p>Diese Klasse kapselt Geschäftslogik rund um Spiele:</p>
 * <ul>
 *   <li>Erstellung und Start eines Spiels</li>
 *   <li>Erzeugung neuer Runden inkl. Frageauswahl</li>
 *   <li>Speichern von Antworten und (optional) Rundenauswertung</li>
 *   <li>Aktualisieren von Punkteständen und Spielerstatistiken</li>
 *   <li>Abfrage von Spielen/Runden für Controller und Orchestratoren</li>
 * </ul>
 *
 * <p>Transaktionen: Durch {@code @Transactional} laufen Methoden standardmäßig
 * in einer Transaktion, sodass zusammengehörige Updates konsistent gespeichert werden.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    /**
     * Punkte pro korrekter Antwort.
     */
    private static final int POINTS_PER_CORRECT = 10;

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final RoundRepository roundRepository;

    /**
     * Lädt ein Spiel anhand seiner ID oder wirft eine Exception, wenn es nicht existiert.
     *
     * @param gameId ID des Spiels
     * @return persistiertes {@link Game}
     * @throws RuntimeException wenn das Spiel nicht gefunden wurde
     */
    private Game requireGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));
    }

    /**
     * Lädt eine bestimmte Runde eines Spiels oder wirft eine Exception, wenn sie nicht existiert.
     *
     * @param gameId ID des Spiels
     * @param roundNumber Rundennummer innerhalb des Spiels
     * @return persistierte {@link Round}
     * @throws RuntimeException wenn die Runde nicht gefunden wurde
     */
    private Round requireRound(Long gameId, int roundNumber) {
        return roundRepository.findByGameIdAndRoundNumber(gameId, roundNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Round not found for game: " + gameId + ", round: " + roundNumber));
    }

    /**
     * Lädt einen Spieler anhand seiner ID oder wirft eine Exception mit einer individuellen Nachricht.
     *
     * @param playerId ID des Spielers
     * @param msg Fehlermeldung für den Ausnahmefall
     * @return persistierter {@link Player}
     * @throws RuntimeException wenn der Spieler nicht gefunden wurde
     */
    private Player requirePlayer(Long playerId, String msg) {
        return playerRepository.findById(playerId).orElseThrow(() -> new RuntimeException(msg));
    }

    /**
     * Erstellt ein neues Spiel zwischen zwei Spielern.
     *
     * <p>Sicherheits-/Konsistenzchecks:</p>
     * <ul>
     *   <li>Spieler-IDs dürfen nicht {@code null} sein</li>
     *   <li>Self-Match (player1Id == player2Id) wird blockiert</li>
     *   <li>Beide Spieler müssen existieren</li>
     * </ul>
     *
     * <p>Das Spiel wird initial im Status {@code WAITING} angelegt und mit
     * Score 0:0 initialisiert.</p>
     *
     * @param player1Id ID des ersten Spielers
     * @param player2Id ID des zweiten Spielers
     * @param category Kategorie/Thema des Spiels
     * @return gespeichertes {@link Game}
     * @throws IllegalArgumentException bei ungültigen IDs oder Self-Match
     */
    public Game createGame(Long player1Id, Long player2Id, String category) {
        if (player1Id == null || player2Id == null) {
            throw new IllegalArgumentException("player ids must not be null");
        }
        if (player1Id.equals(player2Id)) {
            throw new IllegalArgumentException("SELF_MATCH blocked: player1Id == player2Id == " + player1Id);
        }

        Player player1 = requirePlayer(player1Id, "Player 1 not found");
        Player player2 = requirePlayer(player2Id, "Player 2 not found");

        LocalDateTime now = LocalDateTime.now();

        Game game = new Game();
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.WAITING);
        game.setStartTime(now);
        game.setScorePlayer1(0);
        game.setScorePlayer2(0);
        game.setCategory(category);

        return gameRepository.save(game);
    }

    /**
     * Setzt ein Spiel auf {@code IN_PROGRESS}.
     *
     * @param gameId ID des Spiels
     * @return aktualisiertes {@link Game}
     * @throws RuntimeException wenn das Spiel nicht existiert
     */
    public Game startGame(Long gameId) {
        Game game = requireGame(gameId);
        game.setStatus(GameStatus.IN_PROGRESS);
        return gameRepository.save(game);
    }

    /**
     * Startet eine neue Runde für ein Spiel und wählt dabei eine Frage aus.
     *
     * <p>Frageauswahl:</p>
     * <ul>
     *   <li>Primär: zufällige Frage passend zur Spiel-Kategorie</li>
     *   <li>Fallback: zufällige Frage aus allen Kategorien, falls keine passende gefunden wird</li>
     * </ul>
     *
     * <p>Die Runde wird mit Startzeit gesetzt und initialen Punkten 0/0 gespeichert.</p>
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der neu zu startenden Runde
     * @return gespeicherte {@link Round}
     * @throws RuntimeException wenn keine Fragen verfügbar sind
     */
    public Round startNewRound(Long gameId, int roundNumber) {
        Game game = requireGame(gameId);

        log.debug("🔍 startNewRound: gameId={}, category='{}', round={}", gameId, game.getCategory(), roundNumber);

        List<Question> questions = questionRepository.findRandomQuestionsByCategory(game.getCategory(), 1);
        log.debug("🔍 Gefundene Fragen mit Kategorie-Filter: {}", questions.size());

        if (questions.isEmpty()) {
            log.warn("⚠️ Keine Fragen gefunden für Kategorie: '{}' -> Fallback zu allen Kategorien", game.getCategory());
            questions = questionRepository.findRandomQuestions(1);
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions available");
        }

        Question question = questions.get(0);
        log.debug("✅ Ausgewählte Frage: '{}' (Kategorie: '{}')", question.getQuestionText(), question.getCategory());

        LocalDateTime now = LocalDateTime.now();

        Round round = new Round();
        round.setGame(game);
        round.setQuestion(question);
        round.setRoundNumber(roundNumber);
        round.setPointsPlayer1(0);
        round.setPointsPlayer2(0);
        round.setStartTime(now);

        return roundRepository.save(round);
    }

    /**
     * Liefert ein Spiel anhand der ID.
     *
     * @param gameId ID des Spiels
     * @return {@link Game}
     * @throws RuntimeException wenn das Spiel nicht existiert
     */
    public Game getGameById(Long gameId) {
        return requireGame(gameId);
    }

    /**
     * Liefert die aktuelle Runde eines Spiels anhand der Rundennummer.
     *
     * @param gameId ID des Spiels
     * @param roundNumber Nummer der Runde
     * @return {@link Round}
     * @throws RuntimeException wenn die Runde nicht existiert
     */
    public Round getCurrentRound(Long gameId, int roundNumber) {
        return requireRound(gameId, roundNumber);
    }

    /**
     * Speichert die Antwort eines Spielers für eine Runde.
     *
     * <p>Die Antwort wird je nach Spielerrolle in {@code answerPlayer1} oder {@code answerPlayer2} gespeichert.</p>
     *
     * @param gameId ID des Spiels
     * @param playerId ID des antwortenden Spielers
     * @param roundNumber Nummer der Runde
     * @param answer gegebene Antwort
     * @throws RuntimeException wenn der Spieler nicht Teil des Spiels ist oder Spiel/Runde nicht existiert
     */
    public void submitAnswer(Long gameId, Long playerId, int roundNumber, String answer) {
        Game game = requireGame(gameId);
        Round round = requireRound(gameId, roundNumber);

        if (game.getPlayer1().getId().equals(playerId)) {
            round.setAnswerPlayer1(answer);
        } else if (game.getPlayer2().getId().equals(playerId)) {
            round.setAnswerPlayer2(answer);
        } else {
            throw new RuntimeException("Player " + playerId + " is not part of game " + gameId);
        }

        roundRepository.save(round);
    }

    /**
     * Berechnet die Punkte für eine Runde und speichert das Rundenergebnis.
     *
     * <p>Regel: pro korrekter Antwort gibt es {@link #POINTS_PER_CORRECT} Punkte, sonst 0.</p>
     *
     * <p>Diese Methode aktualisiert nur die Rundendaten (Punkte, Endzeit).
     * Die Aggregation in den Spiel-Score kann separat erfolgen (z. B. über {@link #updateRoundPoints(Long, int, int)}
     * oder {@link #addScores(Long, int, int)}).</p>
     *
     * @param round zu bewertende Runde
     */
    public void calculateRoundPoints(Round round) {
        String correctAnswer = round.getQuestion().getCorrectAnswer();

        int points1 = correctAnswer.equalsIgnoreCase(round.getAnswerPlayer1()) ? POINTS_PER_CORRECT : 0;
        int points2 = correctAnswer.equalsIgnoreCase(round.getAnswerPlayer2()) ? POINTS_PER_CORRECT : 0;

        round.setPointsPlayer1(points1);
        round.setPointsPlayer2(points2);
        round.setEndTime(LocalDateTime.now());
        roundRepository.save(round);
    }

    /**
     * Aktualisiert die Punkte einer Runde und addiert die Punkte auf den Spiel-Score.
     *
     * <p>Diese Methode setzt die übergebenen Rundepunkte und erhöht anschließend
     * die Gesamtpunkte im zugehörigen {@link Game}.</p>
     *
     * @param roundId ID der Runde
     * @param pointsPlayer1 Punkte für Spieler 1
     * @param pointsPlayer2 Punkte für Spieler 2
     * @throws RuntimeException wenn die Runde nicht existiert
     */
    public void updateRoundPoints(Long roundId, int pointsPlayer1, int pointsPlayer2) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found"));

        round.setPointsPlayer1(pointsPlayer1);
        round.setPointsPlayer2(pointsPlayer2);
        roundRepository.save(round);

        Game game = round.getGame();
        game.setScorePlayer1(game.getScorePlayer1() + pointsPlayer1);
        game.setScorePlayer2(game.getScorePlayer2() + pointsPlayer2);
        gameRepository.save(game);
    }

    /**
     * Liefert alle Runden eines Spiels in aufsteigender Reihenfolge.
     *
     * @param gameId ID des Spiels
     * @return Liste der {@link Round}-Objekte sortiert nach Rundennummer
     */
    public List<Round> getGameRounds(Long gameId) {
        return roundRepository.findByGameIdOrderByRoundNumber(gameId);
    }

    /**
     * Beendet ein Spiel und aktualisiert Statistiken der beteiligten Spieler.
     *
     * <p>Setzt Status auf {@code FINISHED}, setzt {@code endTime} und bestimmt
     * optional den Gewinner (bei Gleichstand bleibt {@code winner} {@code null}).</p>
     *
     * <p>Anschließend werden Spielerstatistiken (Games, Wins/Losses, Highscore) aktualisiert.</p>
     *
     * @param gameId ID des Spiels
     * @return aktualisiertes und gespeichertes {@link Game}
     */
    public Game finishGame(Long gameId) {
        Game game = requireGame(gameId);

        game.setStatus(GameStatus.FINISHED);
        game.setEndTime(LocalDateTime.now());

        if (game.getScorePlayer1() > game.getScorePlayer2()) {
            game.setWinner(game.getPlayer1());
        } else if (game.getScorePlayer2() > game.getScorePlayer1()) {
            game.setWinner(game.getPlayer2());
        } // Bei Gleichstand bleibt winner null

        updatePlayerStats(game);

        return gameRepository.save(game);
    }

    /**
     * Aktualisiert Statistiken der beiden Spieler eines Spiels.
     *
     * <p>Aktualisiert:</p>
     * <ul>
     *   <li>{@code totalGames} für beide Spieler</li>
     *   <li>{@code gamesWon/gamesLost} abhängig vom Gewinner</li>
     *   <li>{@code highscore} (max. aus bisherigem Highscore und aktuellem Spielscore)</li>
     * </ul>
     *
     * @param game abgeschlossenes Spiel
     */
    private void updatePlayerStats(Game game) {
        Player player1 = game.getPlayer1();
        Player player2 = game.getPlayer2();

        player1.setTotalGames(player1.getTotalGames() + 1);
        player2.setTotalGames(player2.getTotalGames() + 1);

        if (game.getWinner() != null) {
            boolean p1Won = game.getWinner().getId().equals(player1.getId());
            if (p1Won) {
                player1.setGamesWon(player1.getGamesWon() + 1);
                player2.setGamesLost(player2.getGamesLost() + 1);
            } else {
                player2.setGamesWon(player2.getGamesWon() + 1);
                player1.setGamesLost(player1.getGamesLost() + 1);
            }
        }

        int p1Score = game.getScorePlayer1();
        int p2Score = game.getScorePlayer2();

        player1.setHighscore(Math.max(player1.getHighscore(), p1Score));
        player2.setHighscore(Math.max(player2.getHighscore(), p2Score));

        playerRepository.save(player1);
        playerRepository.save(player2);
    }

    /**
     * Liefert das aktuell aktive Spiel eines Spielers.
     *
     * <p>Aktiv bedeutet {@code IN_PROGRESS}. Falls kein aktives Spiel existiert,
     * wird {@code null} zurückgegeben.</p>
     *
     * @param playerId ID des Spielers
     * @return aktives {@link Game} oder {@code null}
     */
    public Game getActiveGameByPlayer(Long playerId) {
        return gameRepository.findByPlayerIdAndStatus(playerId, GameStatus.IN_PROGRESS).orElse(null);
    }

    /**
     * Liefert alle Spiele eines Spielers (als Spieler 1 oder Spieler 2).
     *
     * @param playerId ID des Spielers
     * @return Liste aller Spiele des Spielers
     * @throws RuntimeException wenn der Spieler nicht existiert
     */
    public List<Game> getGamesByPlayerId(Long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new RuntimeException("Player not found with id: " + playerId);
        }
        return gameRepository.findByPlayer1IdOrPlayer2Id(playerId, playerId);
    }

    /**
     * Addiert Punkte auf den aktuellen Spiel-Score und speichert das Spiel.
     *
     * @param gameId ID des Spiels
     * @param p1Points zu addierende Punkte für Spieler 1
     * @param p2Points zu addierende Punkte für Spieler 2
     * @return aktualisiertes {@link Game}
     * @throws RuntimeException wenn das Spiel nicht existiert
     */
    public Game addScores(Long gameId, int p1Points, int p2Points) {
        Game game = requireGame(gameId);
        game.setScorePlayer1(game.getScorePlayer1() + p1Points);
        game.setScorePlayer2(game.getScorePlayer2() + p2Points);
        return gameRepository.save(game);
    }
}
