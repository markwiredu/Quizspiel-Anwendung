package paf_grp_k.websocket;

import java.security.Principal;

/**
 * Minimaler Principal für WebSocket-Verbindungen.
 * Enthält die playerId als Name.
 */
public class PlayerPrincipal implements Principal {

    private final String name;

    public PlayerPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
