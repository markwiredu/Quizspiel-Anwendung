package paf_grp_k.model;

/**
 * Definiert die möglichen Statuswerte eines Spiels.
 *
 * <p>Der Status wird verwendet, um den aktuellen Zustand eines Spiels
 * im System zu verfolgen, z. B. ob es auf Spieler wartet, bereits läuft
 * oder beendet ist.</p>
 */
public enum GameStatus {

    /**
     * Das Spiel wurde erstellt, wartet aber noch auf weitere Spieler,
     * bevor es gestartet werden kann.
     *
     */
    WAITING,

    /**
     * Das Spiel läuft aktuell und ist im aktiven Spielverlauf.
     */
    IN_PROGRESS,

    /**
     * Das Spiel wurde beendet – es gibt ein Ergebnis oder es wurde abgebrochen.
     */
    FINISHED
}
