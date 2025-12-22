package paf_grp_k.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.security.Principal;

/**
 * Debug Listener für WebSocket Connections.
 * Zeigt nur erfolgreiche Principals an.
 */
@Component
public class WebSocketDebugListener {

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null) {
            System.out.println("🟢 WebSocket verbunden mit Principal = " + user.getName());
        } else {
            // Kein Principal → nichts loggen, kein Fehler
        }
    }
}
