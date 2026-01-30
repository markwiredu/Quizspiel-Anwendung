package paf_grp_k.dto;

import lombok.Data;

/**
 * DTO zur Übertragung von Login-Daten eines Spielers.
 *
 * <p>Diese Klasse wird bei der Anmeldung eines Spielers verwendet,
 * um die erforderlichen Zugangsdaten vom Client an den Server zu senden.</p>
 *
 * <p>Die enthaltenen Informationen werden serverseitig validiert und
 * zur Authentifizierung des Spielers genutzt.</p>
 */
@Data
public class LoginRequest {

    /**
     * Eindeutige ID des Spielers, der sich anmelden möchte.
     */
    private Long playerId;

    /**
     * Klartext-Passwort des Spielers.
     *
     * <p>Das Passwort wird ausschließlich zum Vergleich mit dem
     * gespeicherten Passwort-Hash verwendet und nicht persistiert.</p>
     */
    private String password;
}
