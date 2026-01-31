package paf_grp_k.websocket;

import java.security.Principal;

/**
 * Minimaler {@link Principal} für WebSocket/STOMP-Verbindungen.
 *
 * <p>Diese Klasse wird genutzt, um eine WebSocket-Session eindeutig einem Spieler zuzuordnen.
 * Der {@link #getName()}-Wert enthält dabei typischerweise die {@code playerId} als String.</p>
 *
 * <p>Hintergrund:</p>
 * <ul>
 *   <li>Spring verwendet {@link Principal#getName()} u. a. für User-Destinations
 *       wie {@code /user/{name}/queue/...}.</li>
 *   <li>Durch das Setzen der playerId als Name können Nachrichten gezielt
 *       an einzelne Spieler gesendet werden.</li>
 * </ul>
 */
public class PlayerPrincipal implements Principal {

    /**
     * Identität des Principals (in diesem Projekt: Spieler-ID als String).
     *
     */
    private final String name;

    /**
     * Erstellt einen {@code PlayerPrincipal} mit einem eindeutigen Namen.
     *
     * <p>In der Regel ist {@code name} die {@code playerId} als String.</p>
     *
     * @param name eindeutiger Name des Principals (z. B. playerId)
     */
    public PlayerPrincipal(String name) {
        this.name = name;
    }

    /**
     * Liefert den eindeutigen Namen dieses Principals.
     *
     * <p>Spring nutzt diesen Wert, um Nachrichten an User-Destinations
     * korrekt zu routen.</p>
     *
     * @return Name/Identität des Principals (z. B. playerId als String)
     */
    @Override
    public String getName() {
        return name;
    }
}
