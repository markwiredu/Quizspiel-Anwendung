package paf_grp_k.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Zentrale Konfigurationsklasse für WebSocket-Kommunikation mit STOMP.
 *
 * <p>Diese Klasse konfiguriert die WebSocket-Infrastruktur der Anwendung
 * einschließlich Message Broker, Zielpräfixe und STOMP-Endpunkte.</p>
 *
 * <p>Durch {@code @EnableWebSocketMessageBroker} wird die Unterstützung für
 * STOMP-basierte Nachrichten über WebSockets aktiviert.</p>
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Konfiguriert den Message Broker für die asynchrone Nachrichtenverarbeitung.
     *
     * <p>Die Konfiguration legt fest:</p>
     * <ul>
     *     <li>welche Zielpräfixe vom internen Simple Broker verarbeitet werden</li>
     *     <li>welche Präfixe für anwendungsinterne Nachrichten (Controller) gelten</li>
     *     <li>welches Präfix für benutzerspezifische Nachrichten verwendet wird</li>
     * </ul>
     *
     * <p>Beispiel:
     * Nachrichten an {@code /topic/updates} werden an abonnierte Clients verteilt,
     * während {@code /app/send} an einen {@code @MessageMapping}-Controller geht.</p>
     *
     * @param registry Registry zur Konfiguration des Message Brokers
     *
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Registriert die STOMP-Endpunkte für WebSocket-Verbindungen.
     *
     * <p>Diese Endpunkte dienen als Einstiegspunkte für Clients, um eine
     * WebSocket- bzw. SockJS-Verbindung zur Anwendung aufzubauen.</p>
     *
     * <p>Es werden jeweils zwei Varianten bereitgestellt:</p>
     * <ul>
     *     <li>mit SockJS als Fallback für ältere Browser oder restriktive Netzwerke</li>
     *     <li>ohne SockJS für moderne Browser mit nativer WebSocket-Unterstützung</li>
     * </ul>
     *
     * <p>Die Verwendung von {@code setAllowedOriginPatterns("*")} erlaubt
     * Zugriffe von beliebigen Origins (z. B. getrenntes Frontend).</p>
     *
     * @param registry Registry zur Registrierung von STOMP-Endpunkten
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("Registriere WebSocket-Endpunkte");

        // Endpunkt: /ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Endpunkt: /quiz-websocket
        registry.addEndpoint("/quiz-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Alternative ohne SockJS (native WebSockets)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/quiz-websocket")
                .setAllowedOriginPatterns("*");

        log.info("WebSocket-Endpunkte erfolgreich registriert");
    }
}
