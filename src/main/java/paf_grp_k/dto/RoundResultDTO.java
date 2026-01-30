package paf_grp_k.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) für das Ergebnis einer Spielrunde.
 *
 * <p>Dieses DTO fasst alle relevanten Informationen zusammen,
 * die Clients nach Abschluss einer Runde benötigen, z. B.:</p>
 * <ul>
 *   <li>welche Antwort korrekt war</li>
 *   <li>wie viele Punkte die Spieler in dieser Runde erhalten haben</li>
 *   <li>ob das Spiel nach dieser Runde beendet ist</li>
 * </ul>
 *
 * <p>Typische Verwendung:</p>
 * <ul>
 *   <li>WebSocket-Event {@code ROUND_RESULT}</li>
 *   <li>Frontend-Anzeige des Rundenergebnisses</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoundResultDTO {

    /**
     * ID des zugehörigen Spiels.
     */
    private Long gameId;

    /**
     * Nummer der Runde, deren Ergebnis dargestellt wird.
     */
    private int roundNumber;

    /**
     * Die korrekte Antwort der Frage (z. B. "A", "B", "C", "D").
     *
     * <p>Wird erst nach Abschluss der Runde an die Clients gesendet.</p>
     */
    private String correctAnswer;

    /**
     * Punkte, die Spieler 1 in dieser Runde erzielt hat.
     */
    private int player1Points;

    /**
     * Punkte, die Spieler 2 in dieser Runde erzielt hat.
     */
    private int player2Points;

    /**
     * Nachricht zur Anzeige im Frontend
     * (z. B. "Runde 3 beendet!").
     */
    private String message;

    /**
     * Gibt an, ob das Spiel nach dieser Runde beendet ist.
     *
     * <p>{@code true}, wenn dies die letzte Runde war.</p>
     */
    private boolean isGameFinished;
}
