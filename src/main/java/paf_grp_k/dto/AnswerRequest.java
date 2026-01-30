package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO zur Übertragung einer Spielerantwort innerhalb einer Spielrunde.
 *
 * <p>Diese Klasse wird bei der WebSocket-Kommunikation verwendet, um
 * die Antwort eines Spielers an den Server zu senden.</p>
 *
 * <p>Das Objekt enthält alle notwendigen Informationen, um die Antwort
 * einer bestimmten Runde eindeutig einem Spieler und einem Spiel
 * zuzuordnen.</p>
 */
@Getter
@Setter
public class AnswerRequest {

    /**
     * Eindeutige ID des Spiels, zu dem die Antwort gehört.
     */
    private Long gameId;

    /**
     * Eindeutige ID des Spielers, der die Antwort abgegeben hat.
     */
    private Long playerId;

    /**
     * Nummer der Spielrunde, in der die Antwort abgegeben wurde.
     */
    private int roundNumber;

    /**
     * Vom Spieler übermittelte Antwort.
     */
    private String answer;
}
