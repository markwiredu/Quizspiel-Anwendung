package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO zur Übermittlung des aktuellen Lobby-Status an den Client.
 *
 * <p>Dieses Objekt wird typischerweise über WebSockets an den Client gesendet,
 * um ihn über den Zustand des Matchmakings oder der Lobby zu informieren.</p>
 *
 * <p>Mögliche Statuswerte:</p>
 * <ul>
 *     <li>{@code WAITING} – Spieler wartet auf einen Gegner</li>
 *     <li>{@code MATCHED} – Gegner gefunden, Spiel wird erstellt</li>
 *     <li>{@code LEFT} – Spieler hat die Lobby verlassen</li>
 *     <li>{@code GAME_STARTING} – Spiel startet in Kürze</li>
 *     <li>{@code ALREADY_IN_LOBBY} – Spieler befindet sich bereits in einer Lobby</li>
 *     <li>{@code ERROR} – Fehlerzustand</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LobbyStatusDTO {

    /**
     * Aktueller Status der Lobby bzw. des Matchmakings.
     */
    private String status;

    /**
     * Aktuelle Position des Spielers in der Warteschlange.
     */
    private int positionInQueue;

    /**
     * Gesamtanzahl der Spieler in der Warteschlange.
     */
    private int totalPlayersInQueue;

    /**
     * ID des zugewiesenen Spiels (nur relevant bei {@code MATCHED}).
     */
    private Long gameId;

    /**
     * Benutzerfreundliche Statusnachricht für den Client.
     */
    private String message;

    /**
     * Kategorie oder Themengebiet des Matchmakings.
     */
    private String category;

    /**
     * Konstruktor ohne {@code gameId}.
     *
     * <p>Wird verwendet, wenn noch kein Spiel erstellt wurde
     * oder die Spiel-ID für den aktuellen Status nicht relevant ist.</p>
     *
     * @param status aktueller Lobby-Status
     * @param positionInQueue Position des Spielers in der Warteschlange
     * @param totalPlayersInQueue Anzahl aller wartenden Spieler
     * @param message Statusnachricht für den Client
     * @param category Kategorie des Spiels
     */
    public LobbyStatusDTO(
            String status,
            int positionInQueue,
            int totalPlayersInQueue,
            String message,
            String category) {

        this.status = status;
        this.positionInQueue = positionInQueue;
        this.totalPlayersInQueue = totalPlayersInQueue;
        this.message = message;
        this.category = category;
    }

    /**
     * Erstellt einen Status für wartende Spieler.
     *
     * @param position aktuelle Position in der Warteschlange
     * @param total Gesamtanzahl der wartenden Spieler
     * @param category gewählte Spielkategorie
     * @return {@link LobbyStatusDTO} mit Status {@code WAITING}
     */
    public static LobbyStatusDTO waiting(
            int position,
            int total,
            String category) {

        return new LobbyStatusDTO(
                "WAITING",
                position,
                total,
                "Warte auf Gegner... Position: " + position + " von " + total,
                category
        );
    }

    /**
     * Erstellt einen Fehlerstatus für die Lobby.
     *
     * @param message Fehlermeldung für den Client
     * @return {@link LobbyStatusDTO} mit Status {@code ERROR}
     */
    public static LobbyStatusDTO error(String message) {
        return new LobbyStatusDTO(
                "ERROR",
                0,
                0,
                message,
                null
        );
    }

    /**
     * Erstellt einen Status, wenn sich der Spieler bereits in einer Lobby befindet.
     *
     * @param category aktuelle Spielkategorie
     * @return {@link LobbyStatusDTO} mit Status {@code ALREADY_IN_LOBBY}
     */
    public static LobbyStatusDTO alreadyInLobby(String category) {
        return new LobbyStatusDTO(
                "ALREADY_IN_LOBBY",
                0,
                0,
                "Du bist bereits in einer Lobby!",
                category
        );
    }

    /**
     * Erstellt einen Status nach erfolgreichem Matchmaking.
     *
     * <p>In diesem Zustand wurde ein Gegner gefunden und ein neues Spiel erstellt.</p>
     *
     * @param gameId ID des neu erstellten Spiels
     * @param category Spielkategorie
     * @return {@link LobbyStatusDTO} mit Status {@code MATCHED}
     */
    public static LobbyStatusDTO matched(Long gameId, String category) {
        return new LobbyStatusDTO(
                "MATCHED",
                0,
                0,
                "Gegner gefunden! Spiel startet gleich...",
                category
        );
    }
}
