package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * WebSocket-Nachricht zur Benachrichtigung über ein erfolgreiches Matchmaking.
 *
 * <p>Diese Nachricht wird vom Server an Clients gesendet, sobald zwei Spieler
 * erfolgreich zu einem Spiel zusammengeführt wurden.</p>
 *
 * <p>Sie enthält alle notwendigen Informationen, damit der Client
 * in den Spielkontext wechseln kann.</p>
 */
@Getter
@Setter
public class GameMatchMessage {

    /**
     * Typ der WebSocket-Nachricht.
     *
     * <p>Dient dem Client zur Unterscheidung verschiedener Nachrichtentypen.</p>
     */
    private String type = "GAME_MATCHED";

    /**
     * Eindeutige ID des neu erstellten Spiels.
     */
    private Long gameId;

    /**
     * Informationen über den gegnerischen Spieler.
     */
    private PlayerDTO opponent;

    /**
     * Kategorie oder Themengebiet des Spiels.
     */
    private String category;

    /**
     * Erstellt eine neue Matchmaking-Nachricht.
     *
     * @param gameId eindeutige ID des neu erstellten Spiels
     * @param opponent DTO mit Informationen über den gegnerischen Spieler
     * @param category Kategorie bzw. Thema des Spiels
     */
    public GameMatchMessage(
            Long gameId,
            PlayerDTO opponent,
            String category) {

        this.gameId = gameId;
        this.opponent = opponent;
        this.category = category;
    }
}
