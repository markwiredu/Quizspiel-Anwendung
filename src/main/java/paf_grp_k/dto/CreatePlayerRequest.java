package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO zur Erstellung eines neuen Spielers.
 *
 * <p>Diese Klasse wird verwendet, um Registrierungsdaten vom Client
 * an den Server zu übertragen. Sie enthält die erforderlichen Informationen,
 * um einen neuen Spieler anzulegen.</p>
 *
 * <p>Die eigentliche Validierung der Felder (z. B. Mindestlänge des Passworts
 * oder Eindeutigkeit des Usernamens) erfolgt serverseitig im Controller
 * oder Service.</p>
 */
@Getter
@Setter
public class CreatePlayerRequest {

    /**
     * Benutzername des neuen Spielers.
     */
    private String username;

    /**
     * Klartext-Passwort des neuen Spielers.
     *
     * <p>Das Passwort wird serverseitig gehasht gespeichert
     * und niemals im Klartext persistiert.</p>
     */
    private String password;

    /**
     * Optionale URL zu einem Profilbild des Spielers.
     *
     * <p>Wird kein Wert angegeben, setzt der Server automatisch
     * einen Standard-Avatar.</p>
     */
    private String profileImageUrl;
}
