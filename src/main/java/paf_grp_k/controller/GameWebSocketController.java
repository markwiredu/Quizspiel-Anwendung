package paf_grp_k.controller;

import paf_grp_k.dto.*;
import paf_grp_k.model.Game;
import paf_grp_k.model.GameStatus;
import paf_grp_k.model.Player;
import paf_grp_k.model.Question;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.*;

/**
 * WebSocket-Controller für das QuizDuell-Spiel.
 *
 * <p>Verarbeitet Lobby-Beitritte, Spielerantworten und Spielstart über STOMP-WebSockets.
 * Verwaltet temporäre Warteschlangen für Spieler-Matching und startet Spiele/Runden.</p>
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    /**
     * Template zum Versenden von Nachrichten an Clients.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Repository für Spieler-Entitäten.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository für Spiel-Entitäten.
     */
    private final GameRepository gameRepository;

    /**
     * Repository für Frage-Entitäten.
     */
    private final QuestionRepository questionRepository;

    /**
     * Lobby-Warteschlangen nach Kategorie für das Spieler-Matching.
     */
    private final Map<String, Queue<Long>> lobbyQueues = new HashMap<>();


    // -----------------------------------------------------
    // STOMP-Mapping Methoden
    // -----------------------------------------------------

    /**
     * Spieler tritt der Warteschlange bei einer bestimmten Kategorie bei.
     *
     * @param request DTO mit Spieler-ID und optionaler Kategorie
     */
    @MessageMapping("/game.join")
    public void joinLobby(@Payload JoinLobbyRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        String category = request.getCategory() != null ? request.getCategory() : "ALL";

        // Spieler zur Warteschlange hinzufügen
        lobbyQueues.putIfAbsent(category, new LinkedList<>());
        Queue<Long> queue = lobbyQueues.get(category);

        if (!queue.contains(request.getPlayerId())) {
            queue.add(request.getPlayerId());
        }

        // Status an Spieler senden
        JoinLobbyResponse response = new JoinLobbyResponse(
                "IN_QUEUE",
                null,
                "Warte auf Gegner... Position: " + (queue.size())
        );
        messagingTemplate.convertAndSendToUser(
                player.getId().toString(),
                "/queue/game.status",
                response
        );

        // Prüfen, ob ein Match möglich ist
        checkForMatch(category);
    }

    /**
     * Spieler sendet eine Antwort während des Spiels.
     *
     * @param request DTO mit Spieler-ID, Spiel-ID und ausgewählter Antwort
     */
    @MessageMapping("/game.answer")
    public void submitAnswer(@Payload PlayerAnswerRequest request) {
        // Vorläufige Verarbeitung
        System.out.println("Spieler " + request.getPlayerId() + " antwortet: " + request.getSelectedAnswer());

        // Bestätigung an Spieler senden
        messagingTemplate.convertAndSendToUser(
                request.getPlayerId().toString(),
                "/queue/game.answer.confirm",
                "Antwort erhalten"
        );
    }


    // -----------------------------------------------------
    // Hilfsmethoden für Lobby und Spielmanagement
    // -----------------------------------------------------

    /**
     * Prüft, ob zwei Spieler für ein Match bereit sind und erstellt ggf. ein Spiel.
     *
     * @param category Kategorie der Warteschlange
     */
    private void checkForMatch(String category) {
        Queue<Long> queue = lobbyQueues.get(category);
        if (queue != null && queue.size() >= 2) {
            Long player1Id = queue.poll();
            Long player2Id = queue.poll();

            createGame(player1Id, player2Id, category);
        }
    }

    /**
     * Erstellt ein neues Spiel für zwei Spieler.
     *
     * @param player1Id ID des ersten Spielers
     * @param player2Id ID des zweiten Spielers
     * @param category Kategorie des Spiels
     */
    private void createGame(Long player1Id, Long player2Id, String category) {
        Player player1 = playerRepository.findById(player1Id)
                .orElseThrow(() -> new RuntimeException("Player 1 not found"));
        Player player2 = playerRepository.findById(player2Id)
                .orElseThrow(() -> new RuntimeException("Player 2 not found"));

        Game game = new Game();
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameStatus.WAITING);
        game.setStartTime(LocalDateTime.now());
        game = gameRepository.save(game);

        notifyPlayersAboutMatch(game, player1, player2);
    }

    /**
     * Benachrichtigt beide Spieler über das Match und startet das Spiel.
     *
     * @param game Spiel-Entität
     * @param player1 Spieler 1
     * @param player2 Spieler 2
     */
    private void notifyPlayersAboutMatch(Game game, Player player1, Player player2) {
        // Spieler 1 benachrichtigen
        GameStartMessage message1 = new GameStartMessage(
                game.getId(),
                player2.getId(),
                player2.getUsername()
        );
        messagingTemplate.convertAndSendToUser(
                player1.getId().toString(),
                "/queue/game.start",
                message1
        );

        // Spieler 2 benachrichtigen
        GameStartMessage message2 = new GameStartMessage(
                game.getId(),
                player1.getId(),
                player1.getUsername()
        );
        messagingTemplate.convertAndSendToUser(
                player2.getId().toString(),
                "/queue/game.start",
                message2
        );

        startGame(game);
    }

    /**
     * Setzt den Spielstatus auf IN_PROGRESS und startet die erste Runde.
     *
     * @param game Spiel-Entität
     */
    private void startGame(Game game) {
        game.setStatus(GameStatus.IN_PROGRESS);
        gameRepository.save(game);

        startNewRound(game, 1);
    }

    /**
     * Startet eine neue Runde mit einer zufälligen Frage.
     *
     * @param game Spiel-Entität
     * @param roundNumber Nummer der Runde
     */
    private void startNewRound(Game game, int roundNumber) {
        List<Question> questions = questionRepository.findRandomQuestions(1);
        if (questions.isEmpty()) {
            endGame(game);
            return;
        }

        Question question = questions.get(0);

        Map<String, Object> roundMessage = new HashMap<>();
        roundMessage.put("type", "ROUND_START");
        roundMessage.put("roundNumber", roundNumber);
        roundMessage.put("question", question);
        roundMessage.put("timeLimit", 30000); // 30 Sekunden

        messagingTemplate.convertAndSend(
                "/topic/game." + game.getId(),
                roundMessage
        );
    }

    /**
     * Beendet ein Spiel, setzt den Status auf FINISHED und benachrichtigt Spieler.
     *
     * @param game Spiel-Entität
     */
    private void endGame(Game game) {
        game.setStatus(GameStatus.FINISHED);
        game.setEndTime(LocalDateTime.now());
        gameRepository.save(game);

        Map<String, Object> endMessage = new HashMap<>();
        endMessage.put("type", "GAME_END");
        endMessage.put("message", "Spiel beendet - keine Fragen verfügbar");

        messagingTemplate.convertAndSend(
                "/topic/game." + game.getId(),
                endMessage
        );
    }
}