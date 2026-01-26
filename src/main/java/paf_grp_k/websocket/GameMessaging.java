package paf_grp_k.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameMessaging {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToUser(Long playerId, String destination, Object payload) {
        messagingTemplate.convertAndSendToUser(playerId.toString(), destination, payload);
    }

    public void sendToTopic(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload);
    }

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
