package paf_grp_k.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Spring-Konfigurationsklasse zur Bereitstellung kryptografischer Komponenten.
 *
 * <p>Diese Klasse definiert sicherheitsrelevante Beans, die zentral in der
 * gesamten Anwendung verwendet werden können. Aktuell stellt sie einen
 * {@link BCryptPasswordEncoder} zur sicheren Passwortverarbeitung bereit.</p>
 *
 * <p>Durch die Verwendung von {@code @Configuration} wird diese Klasse beim
 * Start der Anwendung automatisch von Spring erkannt und initialisiert.</p>
 */
@Configuration
public class CryptoConfig {

    /**
     * Erstellt einen {@link BCryptPasswordEncoder} und registriert ihn als Spring Bean.
     *
     * <p>Der BCrypt-Algorithmus ist speziell für das Hashen von Passwörtern
     * entwickelt worden und bietet Schutz gegen Brute-Force- und Rainbow-Table-Angriffe.</p>
     *
     * <p>Die Bean kann per Dependency Injection (z. B. {@code @Autowired})
     * in Services oder Controllern verwendet werden, um:</p>
     * <ul>
     *     <li>Passwörter sicher zu hashen</li>
     *     <li>eingegebene Passwörter mit gespeicherten Hashes zu vergleichen</li>
     * </ul>
     *
     * @return eine Instanz von {@code BCryptPasswordEncoder} zur sicheren Passwortverarbeitung
     *
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
