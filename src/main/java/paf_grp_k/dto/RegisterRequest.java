package paf_grp_k.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO zur Registrierung eines neuen Spielers.
 *
 * <p>Diese Klasse wird verwendet, um Registrierungsdaten vom Client
 * an den Server zu übertragen. Sie enthält die notwendigen Informationen,
 * um einen neuen Spieler anzulegen.</p>
 *
 * <p>Die Validierung der Felder (z. B. Passwortlänge oder
 * Eindeutigkeit des Usernamens) erfolgt serverseitig.</p>
 */
@Getter
@Setter
public class RegisterRequest {

    /**
     * Benutzername des neuen Spielers.
     *
     */
    private String username;

    /**
     * Klartext-Passwort des neuen Spielers.
     *
     * <p>Das Passwort wird serverseitig gehasht gespeichert
     * und niemals im Klartext persistiert oder zurückgegeben.</p>
     */
    private String password;

    /**
     * Optionale URL zu einem Profilbild des Spielers.
     *
     * <p>Wird kein Wert angegeben, kann serverseitig
     * ein Standard-Avatar gesetzt werden.</p>
     */
    private String profileImageUrl;
}
