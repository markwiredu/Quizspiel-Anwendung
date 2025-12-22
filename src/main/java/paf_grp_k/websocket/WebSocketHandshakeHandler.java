package paf_grp_k.websocket;

import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.security.Principal;
import java.util.Map;

/**
 * Custom HandshakeHandler, der beim WebSocket-Connect einen PlayerPrincipal setzt.
 * Nutzt den Query-Parameter "playerId" als User-Identifikation.
 */
public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        String query = request.getURI().getQuery(); // z.B. "playerId=42"
        if (query != null && query.startsWith("playerId=")) {
            String playerId = query.substring("playerId=".length());
            return new PlayerPrincipal(playerId);
        }

        return super.determineUser(request, wsHandler, attributes);
    }
}
