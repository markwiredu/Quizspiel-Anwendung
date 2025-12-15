
package paf_grp_k.service;

import paf_grp_k.model.*;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import paf_grp_k.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final QuestionRepository questionRepository;
    private final RoundRepository roundRepository;

    // Deine Methoden bleiben gleich...
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

        List<Question> questions = questionRepository.findRandomQuestionsByCategory(
                game.getCategory(), 1
        );

        if (questions.isEmpty()) {
            questions = questionRepository.findRandomQuestions(1);
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions available");
        }

        Question question = questions.get(0);

        Round round = new Round();
        round.setGame(game);
        round.setQuestion(question);
        round.setRoundNumber(roundNumber);
        round.setPointsPlayer1(0);
        round.setPointsPlayer2(0);

        return roundRepository.save(round);
    }

    /**
     * Punkte für Runde berechnen
     */
    private void calculateRoundPoints(Round round) {
        String correctAnswer = round.getQuestion().getCorrectAnswer();

        int points1 = correctAnswer.equals(round.getAnswerPlayer1()) ? 10 : 0;
        int points2 = correctAnswer.equals(round.getAnswerPlayer2()) ? 10 : 0;

        round.setPointsPlayer1(points1);
        round.setPointsPlayer2(points2);

        // Punkte zum Spiel-Score hinzufügen
        Game game = round.getGame();
        game.setScorePlayer1(game.getScorePlayer1() + points1);
        game.setScorePlayer2(game.getScorePlayer2() + points2);
        gameRepository.save(game);
    }

    /**
     * Spiel beenden
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

        return gameRepository.save(game);
    }
}