package paf_grp_k.config;

// Diese Imports brauchen wir, um Spring zu sagen,
// dass diese Klasse eine Konfigurationsklasse ist
// und um den Passwort-Encoder zu verwenden.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Diese Klasse stellt einen Passwort-Encoder für das ganze Projekt bereit.
 *
 * Ziel:
 * - Passwörter werden NICHT im Klartext gespeichert
 * - Stattdessen werden sie sicher gehasht (BCrypt)
 *
 * Diese Konfiguration wird beim Start der Anwendung von Spring geladen.
 */

@Configuration
public class CryptoConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
