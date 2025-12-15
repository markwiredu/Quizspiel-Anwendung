package paf_grp_k.controller;

import paf_grp_k.dto.*;
import paf_grp_k.model.Game;
import paf_grp_k.model.Player;
import paf_grp_k.model.Question;
import paf_grp_k.repository.GameRepository;
import paf_grp_k.repository.PlayerRepository;
import paf_grp_k.repository.QuestionRepository;
import paf_grp_k.service.GameService;
import paf_grp_k.service.LobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.*;

/**
 * WebSocket-Controller für das QuizDuell-Spiel.
 * <p>
 * Dieser Controller verarbeitet WebSocket-Nachrichten für Lobby-Management,
 * Matchmaking, Spielstart, Rundensteuerung und Spielerantworten.
 */
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    /**
     * Template zum Senden von WebSocket-Nachrichten an Benutzer oder Topics.
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Repository für Spielerzugriffe.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository für Spiele.
     */
    private final GameRepository gameRepository;

    /**
     * Repository für Fragen.
     */
    private final QuestionRepository questionRepository;

    /**
     * Service zur Verwaltung der Lobby und des Matchmakings.
     */
    private final LobbyService lobbyService;

    /**
     * Service zur Spiellogik (Spielstart, Runden, Spielende).
     */
    private final GameService gameService;

    /**
     * WebSocket-Endpunkt zum Beitreten einer Lobby.
     * <p>
     * Der Spieler wird einer Lobby (optional mit Kategorie) hinzugefügt.
     * Anschließend wird geprüft, ob ein Match zustande kommt.
     *
     * @param request JoinLobbyRequest mit Spieler-ID und Kategorie
     */
    @MessageMapping("/game.join")
    public void joinLobby(@Payload JoinLobbyRequest request) {
        Player player = playerRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found"));

        String category = request.getCategory() != null ? request.getCategory() : "ALL";

        LobbyStatusDTO lobbyStatus = lobbyService.joinLobby(request.getPlayerId(), category);

        messagingTemplate.convertAndSendToUser(
                player.getId().toString(),
                "/queue/lobby.status",
                lobbyStatus
        );

        checkForMatchAndStartGame(category);
    }

    /**
     * WebSocket-Endpunkt zum Verlassen der Lobby.
     *
     * @param request JoinLobbyRequest mit Spieler-ID und Kategorie
     */
    @MessageMapping("/game.leave")
    public void leaveLobby(@Payload JoinLobbyRequest request) {
        lobbyService.leaveLobby(request.getPlayerId(), request.getCategory());

        LobbyStatusDTO status = new LobbyStatusDTO("LEFT", "Lobby verlassen");
        messagingTemplate.convertAndSendToUser(
                request.getPlayerId().toString(),
                "/queue/lobby.status",
                status
        );
    }

    /**
     * WebSocket-Endpunkt zum Absenden einer Spielerantwort.
     * <p>
     * Die Antwort wird bestätigt und anschließend an alle Spielteilnehmer
     * über ein Topic gesendet.
     *
     * @param request PlayerAnswerRequest mit Antwortdaten
     */
    @MessageMapping("/game.answer")
    public void submitAnswer(@Payload PlayerAnswerRequest request) {
        try {
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/game.answer.confirm",
                    "Antwort erhalten"
            );

            Map<String, Object> answerMessage = new HashMap<>();
            answerMessage.put("type", "ANSWER_SUBMITTED");
            answerMessage.put("playerId", request.getPlayerId());
            answerMessage.put("roundNumber", request.getRoundNumber());
            answerMessage.put("answer", request.getSelectedAnswer());

            messagingTemplate.convertAndSend(
                    "/topic/game." + request.getGameId(),
                    answerMessage
            );

        } catch (Exception e) {
            messagingTemplate.convertAndSendToUser(
                    request.getPlayerId().toString(),
                    "/queue/game.error",
                    "Fehler bei der Antwortverarbeitung: " + e.getMessage()
            );
        }
    }

    /**
     * Prüft, ob in der Lobby ein Match entstanden ist, und startet ggf. ein Spiel.
     *
     * @param category Spielkategorie
     */
    private void checkForMatchAndStartGame(String category) {
        Optional<LobbyService.MatchResult> matchOpt = lobbyService.checkForMatch(category);

        if (matchOpt.isPresent()) {
            LobbyService.MatchResult match = matchOpt.get();

            try {
                Game game = gameService.createGame(match.player1Id, match.player2Id, match.category);
                notifyPlayersAboutMatch(game, match.player1Id, match.player2Id);
                startGameWithDelay(game);

            } catch (Exception e) {
                lobbyService.joinLobby(match.player1Id, category);
                lobbyService.joinLobby(match.player2Id, category);
            }
        }
    }

    /**
     * Informiert beide Spieler über ein erfolgreiches Match.
     *
     * @param game       Das erstellte Spiel
     * @param player1Id  ID von Spieler 1
     * @param player2Id  ID von Spieler 2
     */
    private void notifyPlayersAboutMatch(Game game, Long player1Id, Long player2Id) {
        Player player1 = playerRepository.findById(player1Id).orElseThrow();
        Player player2 = playerRepository.findById(player2Id).orElseThrow();

        PlayerDTO opponent1 = convertToPlayerDTO(player2);
        PlayerDTO opponent2 = convertToPlayerDTO(player1);

        GameMatchMessage message1 = new GameMatchMessage(game.getId(), opponent1, game.getCategory());
        GameMatchMessage message2 = new GameMatchMessage(game.getId(), opponent2, game.getCategory());

        messagingTemplate.convertAndSendToUser(player1Id.toString(), "/queue/game.match", message1);
        messagingTemplate.convertAndSendToUser(player2Id.toString(), "/queue/game.match", message2);
    }

    /**
     * Startet das Spiel nach einem Countdown in einem separaten Thread.
     *
     * @param game Das zu startende Spiel
     */
    private void startGameWithDelay(Game game) {
        new Thread(() -> {
            try {
                for (int i = 5; i > 0; i--) {
                    Map<String, Object> countdownMessage = new HashMap<>();
                    countdownMessage.put("type", "COUNTDOWN");
                    countdownMessage.put("seconds", i);

                    messagingTemplate.convertAndSendToUser(
                            game.getPlayer1().getId().toString(),
                            "/queue/game.countdown",
                            countdownMessage
                    );
                    messagingTemplate.convertAndSendToUser(
                            game.getPlayer2().getId().toString(),
                            "/queue/game.countdown",
                            countdownMessage
                    );

                    Thread.sleep(1000);
                }

                gameService.startGame(game.getId());
                startNewRound(game, 1);

            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Startet eine neue Spielrunde und sendet eine Frage an alle Spieler.
     *
     * @param game        Aktuelles Spiel
     * @param roundNumber Nummer der Runde
     */
    private void startNewRound(Game game, int roundNumber) {
        try {
            gameService.startNewRound(game.getId(), roundNumber);

            List<Question> questions = questionRepository.findRandomQuestionsByCategory(
                    game.getCategory(), 1
            );

            if (questions.isEmpty()) {
                questions = questionRepository.findRandomQuestions(1);
            }

            if (questions.isEmpty()) {
                endGame(game, "Spiel beendet - keine Fragen verfügbar");
                return;
            }

            Map<String, Object> roundMessage = new HashMap<>();
            roundMessage.put("type", "ROUND_START");
            roundMessage.put("roundNumber", roundNumber);
            roundMessage.put("question", questions.get(0));
            roundMessage.put("timeLimit", 30000);

            messagingTemplate.convertAndSend("/topic/game." + game.getId(), roundMessage);

        } catch (Exception e) {
            endGame(game, "Fehler beim Rundenstart");
        }
    }

    /**
     * Beendet das Spiel und sendet die Endergebnisse an alle Spieler.
     *
     * @param game    Das Spiel
     * @param message Abschlussnachricht
     */
    private void endGame(Game game, String message) {
        try {
            gameService.finishGame(game.getId());

            Map<String, Object> endMessage = new HashMap<>();
            endMessage.put("type", "GAME_END");
            endMessage.put("message", message);

            messagingTemplate.convertAndSend("/topic/game." + game.getId(), endMessage);

        } catch (Exception ignored) {
        }
    }

    /**
     * Konvertiert ein Player-Objekt in ein PlayerDTO.
     *
     * @param player Spieler
     * @return PlayerDTO
     */
    private PlayerDTO convertToPlayerDTO(Player player) {
        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setUsername(player.getUsername());
        dto.setProfileImageUrl(player.getProfileImageUrl());
        dto.setTotalGames(player.getTotalGames());
        dto.setGamesWon(player.getGamesWon());
        dto.setGamesLost(player.getGamesLost());
        dto.setHighscore(player.getHighscore());
        return dto;
    }
}
