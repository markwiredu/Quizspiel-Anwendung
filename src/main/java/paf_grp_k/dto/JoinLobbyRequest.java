package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * DTO zur Anfrage eines Spielers, einer Lobby beizutreten.
 *
 * <p>Diese Klasse wird im Rahmen der WebSocket-Kommunikation verwendet,
 * wenn ein Spieler dem Matchmaking oder einer Spiellobby beitreten möchte.</p>
 *
 * <p>Die enthaltenen Informationen ermöglichen es dem Server,
 * den Spieler eindeutig zu identifizieren und einem passenden
 * Spiel oder Gegner zuzuordnen.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class JoinLobbyRequest {

    /**
     * Eindeutige ID des Spielers, der der Lobby beitreten möchte.
     *
     */
    private Long playerId;

    /**
     * Benutzername des Spielers.
     *
     * <p>Wird z. B. für Anzeigezwecke oder zur Identifikation
     * im Lobby- bzw. Matchmaking-Kontext verwendet.</p>
     */
    private String username;

    /**
     * Gewählte Kategorie oder Themengebiet für das Spiel.
     *
     * <p>Diese Information wird verwendet, um Spieler mit
     * ähnlichen Interessen zusammenzuführen.</p>
     */
    private String category;
}
