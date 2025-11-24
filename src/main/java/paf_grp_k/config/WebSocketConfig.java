package paf_grp_k.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Konfiguriert WebSocket und STOMP-Messaging für das QuizDuell-Projekt.
 *
 * <p>Ermöglicht bidirektionale Kommunikation zwischen Client und Server
 * über WebSockets und bietet Fallbacks für Browser ohne WebSocket-Unterstützung.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Konfiguriert den Message Broker für STOMP-Nachrichten.
     *
     * @param config MessageBrokerRegistry zum Einrichten von Präfixen
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Präfix für Nachrichten, die an den Server gesendet werden
        config.setApplicationDestinationPrefixes("/app");

        // Präfixe für Nachrichten, die an Clients gesendet werden
        config.enableSimpleBroker("/topic", "/queue");

        // Präfix für private Nachrichten an bestimmte Benutzer
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registriert den STOMP-Endpunkt für WebSocket-Verbindungen.
     *
     * @param registry StompEndpointRegistry zum Hinzufügen von Endpunkten
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket-Endpunkt für Clients
        registry.addEndpoint("/quiz-websocket")
                .setAllowedOriginPatterns("*") // erlaubt Cross-Origin-Anfragen
                .withSockJS(); // Fallback für Browser ohne WebSocket-Unterstützung
    }
}
