package paf_grp_k.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Zentrale Hilfsklasse für das Versenden von WebSocket-Nachrichten.
 *
 * <p>Diese Komponente kapselt den Zugriff auf {@link SimpMessagingTemplate}
 * und stellt einfache, wiederverwendbare Methoden bereit, um:</p>
 * <ul>
 *   <li>Nachrichten an einzelne Benutzer (User-Destinations)</li>
 *   <li>Nachrichten an Topics (Broadcasts)</li>
 *   <li>Fehlermeldungen an Clients</li>
 * </ul>
 *
 * <p>Die eigentliche Spiellogik befindet sich in Orchestratoren und Services;
 * diese Klasse ist ausschließlich für den Transport der Nachrichten zuständig.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameMessaging {

    /**
     * Spring-Komponente zum Versenden von STOMP/WebSocket-Nachrichten.
     *
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sendet eine Nachricht an einen bestimmten Spieler.
     *
     * <p>Die Nachricht wird an eine User-Destination gesendet
     * (z. B. {@code /user/{playerId}/queue/...}).</p>
     *
     * @param playerId ID des Zielspielers
     * @param destination Zielpfad (z. B. {@code "/queue/game"})
     * @param payload zu sendendes Objekt (wird automatisch serialisiert)
     */
    public void sendToUser(Long playerId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(playerId.toString(), destination, payload);
    }

    /**
     * Sendet eine Nachricht an ein Topic (Broadcast).
     *
     * <p>Alle Clients, die dieses Topic abonniert haben,
     * erhalten die Nachricht.</p>
     *
     * @param destination Ziel-Topic (z. B. {@code "/topic/lobby"})
     * @param payload zu sendendes Objekt
     */
    public void sendToTopic(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

    /**
     * Sendet eine standardisierte Fehlermeldung an einen Spieler.
     *
     * <p>Die Fehlermeldung wird an {@code /queue/errors} gesendet
     * und enthält einen Typ, eine Nachricht sowie einen Timestamp.</p>
     *
     * <p>Diese Methode sollte für alle spiel- oder lobbybezogenen Fehler
     * verwendet werden, die dem Client angezeigt werden sollen.</p>
     *
     * @param playerId ID des Spielers
     * @param message Fehlermeldungstext
     */
    public void sendError(Long playerId, String message) {
        Map<String, Object> error = Map.of(
                "type", "ERROR",
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        sendToUser(playerId, "/queue/errors", error);
        log.warn("⚠️ Fehler an Spieler {} gesendet: {}", playerId, message);
    }
}
