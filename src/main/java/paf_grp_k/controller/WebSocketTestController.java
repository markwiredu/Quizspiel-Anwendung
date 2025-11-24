package paf_grp_k.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

@Controller
public class WebSocketTestController {

    @MessageMapping("/test")  // Empfängt Nachrichten an /app/test
    @SendTo("/topic/test")    // Sendet Antwort an /topic/test
    public String handleTestMessage(String message) {
        System.out.println("🔵 WebSocket Nachricht empfangen: " + message);

        String response = "Server antwortet: " + HtmlUtils.htmlEscape(message)
                + " (Zeit: " + java.time.LocalTime.now() + ")";

        return response;
    }
}