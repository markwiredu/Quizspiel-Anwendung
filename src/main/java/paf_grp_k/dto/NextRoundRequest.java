package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO zur Anforderung des Starts der nächsten Spielrunde.
 *
 * <p>Diese Klasse wird im Rahmen der WebSocket-Kommunikation verwendet,
 * um dem Server mitzuteilen, dass in einem laufenden Spiel
 * zur nächsten Runde gewechselt werden soll.</p>
 *
 * <p>Die Anfrage wird typischerweise vom Host oder nach Abschluss
 * einer Runde ausgelöst.</p>
 */
@Getter
@Setter
public class NextRoundRequest {

    /**
     * Eindeutige ID des Spiels, dessen nächste Runde gestartet werden soll.
     */
    private Long gameId;

    /**
     * Nummer der aktuell abgeschlossenen Spielrunde.
     *
     * <p>Der Server verwendet diesen Wert, um die korrekte
     * nächste Runde zu bestimmen.</p>
     */
    private int roundNumber;
}
