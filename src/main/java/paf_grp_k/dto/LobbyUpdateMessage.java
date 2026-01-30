package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket-Nachricht zur Information über Änderungen in einer Lobby-Warteschlange.
 *
 * <p>Diese Nachricht wird vom Server an verbundene Clients gesendet,
 * um sie über Ereignisse innerhalb einer Lobby zu informieren,
 * z. B. wenn Spieler beitreten oder die Warteschlangenposition
 * aktualisiert wird.</p>
 *
 * <p>Mögliche Statuswerte:</p>
 * <ul>
 *     <li>{@code PLAYER_JOINED} – ein Spieler ist der Lobby beigetreten</li>
 *     <li>{@code PLAYER_LEFT} – ein Spieler hat die Lobby verlassen</li>
 *     <li>{@code MATCH_FOUND} – ein passender Gegner wurde gefunden</li>
 *     <li>{@code QUEUE_UPDATE} – Aktualisierung der Warteschlangenposition</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobbyUpdateMessage {

    /**
     * Kategorie oder Themengebiet der Lobby.
     */
    private String category;

    /**
     * Aktuelle Anzahl der Spieler in der Warteschlange.
     */
    private int playersInQueue;

    /**
     * Aktuelle Position des Spielers in der Warteschlange.
     */
    private int positionInQueue;

    /**
     * Typ des Lobby-Updates.
     *
     * <p>Dient dem Client zur Interpretation des Ereignisses.</p>
     */
    private String status;

    /**
     * Liefert eine lesbare String-Repräsentation der Nachricht.
     *
     * <p>Wird hauptsächlich für Logging- und Debug-Zwecke verwendet.</p>
     *
     * @return String-Repräsentation dieses {@link LobbyUpdateMessage}-Objekts
     */
    @Override
    public String toString() {
        return "LobbyUpdateMessage{" +
                "category='" + category + '\'' +
                ", playersInQueue=" + playersInQueue +
                ", positionInQueue=" + positionInQueue +
                ", status='" + status + '\'' +
                '}';
    }
}
