package paf_grp_k;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Spring-Boot-Anwendung.
 *
 * <p>Diese Klasse startet die gesamte Anwendung inklusive:</p>
 * <ul>
 *   <li>Spring Context Initialisierung</li>
 *   <li>Auto-Konfiguration von Web, Security, JPA, WebSocket usw.</li>
 *   <li>Scan aller Komponenten im Package {@code paf_grp_k}</li>
 * </ul>
 *
 * <p>Die Annotation {@link SpringBootApplication} fasst folgende Annotationen zusammen:</p>
 * <ul>
 *   <li>{@code @Configuration}</li>
 *   <li>{@code @EnableAutoConfiguration}</li>
 *   <li>{@code @ComponentScan}</li>
 * </ul>
 *
 * <p>Von hier aus wird das gesamte Backend gestartet.</p>
 */
@SpringBootApplication
public class PafApplication {

    /**
     * Hauptmethode zum Starten der Spring-Boot-Anwendung.
     *
     * <p>Spring initialisiert beim Aufruf den Application Context
     * und startet anschließend den eingebetteten Webserver.</p>
     *
     */
    public static void main(String[] args) {
        SpringApplication.run(PafApplication.class, args);
    }
}
