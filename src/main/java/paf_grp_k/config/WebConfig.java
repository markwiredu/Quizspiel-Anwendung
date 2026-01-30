package paf_grp_k.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring-Webkonfiguration zur Bereitstellung statischer Ressourcen.
 *
 * <p>Diese Klasse konfiguriert benutzerdefinierte Resource Handler für die
 * Webanwendung. Konkret wird ein Verzeichnis aus dem Dateisystem so eingebunden,
 * dass dessen Inhalte über HTTP erreichbar sind.</p>
 *
 * <p>Die Klasse implementiert {@link WebMvcConfigurer}, um gezielt das Verhalten
 * von Spring MVC zu erweitern, ohne die Standardkonfiguration zu überschreiben.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registriert einen Resource Handler für hochgeladene Dateien.
     *
     * <p>Alle HTTP-Anfragen, die mit {@code /uploads/} beginnen, werden auf das
     * lokale Verzeichnis {@code uploads/} im Dateisystem abgebildet.</p>
     *
     * <p>Beispiel:
     * Eine Datei {@code uploads/image.png} ist unter
     * {@code http://localhost:8080/uploads/image.png} erreichbar.</p>
     *
     * <p>Hinweis:
     * Das Präfix {@code file:} signalisiert Spring, dass es sich um ein
     * Verzeichnis im Dateisystem und nicht um Klassenpfad-Ressourcen handelt.</p>
     *
     * @param registry Registry zur Verwaltung von Resource Handlern
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
