package paf_grp_k.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.util.StringUtils;

import java.security.Principal;
import java.util.Map;

public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        // 1. Versuche, Spieler-ID aus Query-Parametern zu holen
        String query = request.getURI().getQuery();
        String playerId = null;

        if (StringUtils.hasText(query)) {
            // Extrahiere playerId aus Query-String wie: ws://...?playerId=123
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("playerId=")) {
                    playerId = param.substring("playerId=".length());
                    break;
                }
            }
        }

        // 2. Falls nicht in Query, versuche es aus Headern
        if (playerId == null) {
            playerId = request.getHeaders().getFirst("playerId");
        }

        // 3. Falls immer noch nicht, versuche aus Attributen (für SockJS)
        if (playerId == null && attributes.containsKey("playerId")) {
            playerId = (String) attributes.get("playerId");
        }

        // 4. Principal erstellen
        if (StringUtils.hasText(playerId)) {
            final String finalPlayerId = playerId;
            System.out.println("🎯 WebSocket Handshake: Principal gesetzt für Spieler ID: " + finalPlayerId);

            return new Principal() {
                @Override
                public String getName() {
                    return finalPlayerId;
                }

                @Override
                public String toString() {
                    return "PlayerPrincipal{id='" + finalPlayerId + "'}";
                }
            };
        }

        System.out.println("⚠️  WebSocket Handshake: Keine Spieler-ID gefunden, Principal bleibt null");
        return null;
    }
}