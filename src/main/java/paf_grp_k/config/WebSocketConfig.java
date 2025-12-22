package paf_grp_k.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import paf_grp_k.websocket.WebSocketHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");  // Topics & Queues für Broadcast und User
        config.setApplicationDestinationPrefixes("/app");  // @MessageMapping Prefix
        config.setUserDestinationPrefix("/user");         // convertAndSendToUser Prefix

        System.out.println("✅ Message Broker konfiguriert: /topic, /queue, /app, /user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint mit SockJS
        registry.addEndpoint("/quiz-websocket")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new WebSocketHandshakeHandler())
                .withSockJS();

        // Optionaler Endpoint ohne SockJS für moderne Browser
        registry.addEndpoint("/quiz-websocket")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new WebSocketHandshakeHandler());

        System.out.println("✅ STOMP Endpoints registriert: /quiz-websocket");
    }
}
