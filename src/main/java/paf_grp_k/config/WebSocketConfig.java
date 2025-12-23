package paf_grp_k.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Aktiviert einfachen Broker für /topic und /queue
        config.enableSimpleBroker("/topic", "/queue");
        // Präfix für Nachrichten vom Client zum Server
        config.setApplicationDestinationPrefixes("/app");
        // Optional: User Destination für Principal-spezifische Nachrichten
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket-Endpunkt für SockJS
        registry.addEndpoint("/quiz-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
