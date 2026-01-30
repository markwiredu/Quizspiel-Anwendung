package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO zur Übertragung von Spielerinformationen.
 *
 * <p>Diese Klasse wird verwendet, um Spielerinformationen
 * zwischen Server und Client auszutauschen, insbesondere
 * im Kontext von WebSocket-Nachrichten.</p>
 *
 * <p>Das DTO enthält ausschließlich nicht-sensible Daten
 * und eignet sich sowohl für Kurz- als auch Detaildarstellungen
 * eines Spielers.</p>
 */
@Getter
@Setter
public class PlayerDTO {

    /**
     * Eindeutige ID des Spielers.
     */
    private Long id;

    /**
     * Benutzername des Spielers.
     */
    private String username;

    /**
     * URL zum Profilbild des Spielers.
     */
    private String profileImageUrl;

    /**
     * Gesamtanzahl der gespielten Spiele.
     */
    private int totalGames;

    /**
     * Anzahl der gewonnenen Spiele.
     */
    private int gamesWon;

    /**
     * Anzahl der verlorenen Spiele.
     */
    private int gamesLost;

    /**
     * Höchster erreichter Punktestand des Spielers.
     */
    private int highscore;

    /**
     * Standardkonstruktor.
     *
     * <p>Wird von Frameworks wie Jackson für die
     * Serialisierung und Deserialisierung benötigt.</p>
     */
    public PlayerDTO() {
    }

    /**
     * Convenience-Konstruktor für kompakte Spielerinformationen.
     *
     * <p>Dieser Konstruktor eignet sich z. B. für Matchmaking-
     * oder Lobby-Nachrichten, bei denen nur Basisdaten
     * des Spielers benötigt werden.</p>
     *
     * @param id eindeutige ID des Spielers
     * @param username Benutzername des Spielers
     * @param profileImageUrl URL zum Profilbild
     */
    public PlayerDTO(
            Long id,
            String username,
            String profileImageUrl) {

        this.id = id;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
    }
}
