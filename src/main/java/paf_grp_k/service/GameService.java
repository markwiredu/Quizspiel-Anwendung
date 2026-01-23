package paf_grp_k.service;

import paf_grp_k.model.*;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import paf_grp_k.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final RoundRepository roundRepository;

    public Game createGame(Long player1Id, Long player2Id, String category) {
        Player player1 = playerRepository.findById(player1Id)
                .orElseThrow(() -> new RuntimeException("Player 1 not found"));
        Player player2 = playerRepository.findById(player2Id)
                .orElseThrow(() -> new RuntimeException("Player 2 not found"));

        Game game = new Game();
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.WAITING);
        game.setStartTime(LocalDateTime.now());
        game.setScorePlayer1(0);
        game.setScorePlayer2(0);
        game.setCategory(category);

        return gameRepository.save(game);
    }

    public Game startGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        game.setStatus(GameStatus.IN_PROGRESS);
        return gameRepository.save(game);
    }

    public Round startNewRound(Long gameId, int roundNumber) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        // 🔍 WICHTIG: Debug-Ausgaben
        System.out.println("========================================");
        System.out.println("🔍 DEBUG startNewRound:");
        System.out.println("   Game ID: " + gameId);
        System.out.println("   Game Category: '" + game.getCategory() + "'");
        System.out.println("   Round Number: " + roundNumber);
        System.out.println("========================================");

        List<Question> questions = questionRepository.findRandomQuestionsByCategory(
                game.getCategory(), 1
        );

        System.out.println("🔍 Gefundene Fragen mit Kategorie-Filter: " + questions.size());

        if (questions.isEmpty()) {
            System.out.println("⚠️ KEINE Fragen gefunden für Kategorie: '" + game.getCategory() + "'");
            System.out.println("⚠️ Fallback zu allen Kategorien!");
            questions = questionRepository.findRandomQuestions(1);
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions available");
        }

        Question question = questions.get(0);
        System.out.println("✅ Ausgewählte Frage: " + question.getQuestionText());
        System.out.println("   Frage-Kategorie: '" + question.getCategory() + "'");
        System.out.println("========================================");

        Round round = new Round();
        round.setGame(game);
        round.setQuestion(question);
        round.setRoundNumber(roundNumber);
        round.setPointsPlayer1(0);
        round.setPointsPlayer2(0);
        round.setStartTime(LocalDateTime.now());

        return roundRepository.save(round);
    }
    public Game getGameById(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found with id: " + gameId));
    }

    public Round getCurrentRound(Long gameId, int roundNumber) {
        return roundRepository.findByGameIdAndRoundNumber(gameId, roundNumber)
                .orElseThrow(() -> new RuntimeException("Round not found for game: " + gameId + ", round: " + roundNumber));
    }

    public void submitAnswer(Long gameId, Long playerId, int roundNumber, String answer) {
        Game game = getGameById(gameId);
        Round round = getCurrentRound(gameId, roundNumber);

        // Prüfe welcher Spieler antwortet
        if (game.getPlayer1().getId().equals(playerId)) {
            round.setAnswerPlayer1(answer);
        } else if (game.getPlayer2().getId().equals(playerId)) {
            round.setAnswerPlayer2(answer);
        } else {
            throw new RuntimeException("Player " + playerId + " is not part of game " + gameId);
        }

        roundRepository.save(round);

        // Wenn beide geantwortet haben, Punkte berechnen
        if (round.getAnswerPlayer1() != null && round.getAnswerPlayer2() != null) {
            calculateRoundPoints(round);
        }
    }

    /**
     * Punkte für Runde berechnen (jetzt public für Controller)
     */
    public void calculateRoundPoints(Round round) {
        String correctAnswer = round.getQuestion().getCorrectAnswer();

        int points1 = correctAnswer.equals(round.getAnswerPlayer1()) ? 10 : 0;
        int points2 = correctAnswer.equals(round.getAnswerPlayer2()) ? 10 : 0;

        round.setPointsPlayer1(points1);
        round.setPointsPlayer2(points2);
        round.setEndTime(LocalDateTime.now());

        // Punkte zum Spiel-Score hinzufügen
        Game game = round.getGame();
        game.setScorePlayer1(game.getScorePlayer1() + points1);
        game.setScorePlayer2(game.getScorePlayer2() + points2);

        gameRepository.save(game);
        roundRepository.save(round);
    }

    public void updateRoundPoints(Long roundId, int pointsPlayer1, int pointsPlayer2) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new RuntimeException("Round not found"));

        round.setPointsPlayer1(pointsPlayer1);
        round.setPointsPlayer2(pointsPlayer2);
        roundRepository.save(round);

        // Update game score
        Game game = round.getGame();
        game.setScorePlayer1(game.getScorePlayer1() + pointsPlayer1);
        game.setScorePlayer2(game.getScorePlayer2() + pointsPlayer2);
        gameRepository.save(game);
    }

    public List<Round> getGameRounds(Long gameId) {
        return roundRepository.findByGameIdOrderByRoundNumber(gameId);
    }

    /**
     * Spiel beenden und Statistiken aktualisieren
     */
    public Game finishGame(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Game not found"));

        game.setStatus(GameStatus.FINISHED);
        game.setEndTime(LocalDateTime.now());

        // Gewinner ermitteln
        if (game.getScorePlayer1() > game.getScorePlayer2()) {
            game.setWinner(game.getPlayer1());
        } else if (game.getScorePlayer2() > game.getScorePlayer1()) {
            game.setWinner(game.getPlayer2());
        }
        // Bei Gleichstand bleibt winner null

        // Statistiken aktualisieren
        updatePlayerStats(game);

        return gameRepository.save(game);
    }

    /**
     * Spieler-Statistiken aktualisieren
     */
    private void updatePlayerStats(Game game) {
        Player player1 = game.getPlayer1();
        Player player2 = game.getPlayer2();

        // Gesamtspiele erhöhen
        player1.setTotalGames(player1.getTotalGames() + 1);
        player2.setTotalGames(player2.getTotalGames() + 1);

        // Gewinner/Verlierer aktualisieren
        if (game.getWinner() != null) {
            if (game.getWinner().getId().equals(player1.getId())) {
                player1.setGamesWon(player1.getGamesWon() + 1);
                player2.setGamesLost(player2.getGamesLost() + 1);
                log.info("📊 Spieler {} gewinnt gegen {}", player1.getUsername(), player2.getUsername());
            } else {
                player2.setGamesWon(player2.getGamesWon() + 1);
                player1.setGamesLost(player1.getGamesLost() + 1);
                log.info("📊 Spieler {} gewinnt gegen {}", player2.getUsername(), player1.getUsername());
            }
        } else {
            log.info("📊 Unentschieden zwischen {} und {}", player1.getUsername(), player2.getUsername());
        }

        // Spieler speichern
        playerRepository.save(player1);
        playerRepository.save(player2);

        log.info("✅ Statistiken aktualisiert für Spiel {}: {} (S:{} V:{}) vs {} (S:{} V:{})",
                game.getId(),
                player1.getUsername(), player1.getGamesWon(), player1.getGamesLost(),
                player2.getUsername(), player2.getGamesWon(), player2.getGamesLost());
    }

    public List<Game> getPlayerGames(Long playerId) {
        return gameRepository.findByPlayer1IdOrPlayer2Id(playerId, playerId);
    }

    public Game getActiveGameByPlayer(Long playerId) {
        return gameRepository.findByPlayerIdAndStatus(playerId, GameStatus.IN_PROGRESS)
                .orElse(null);
    }

    public List<Game> getGamesByPlayerId(Long playerId) {
        // Prüfen ob Spieler existiert
        if (!playerRepository.existsById(playerId)) {
            throw new RuntimeException("Player not found with id: " + playerId);
        }

        // Spiele abrufen
        return gameRepository.findByPlayer1IdOrPlayer2Id(playerId, playerId);
    }
}