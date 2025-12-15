package paf_grp_k.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

/**
 * Einfacher WebSocket-Testcontroller.
 * <p>
 * Dient zum Testen der WebSocket-Kommunikation zwischen Client und Server.
 * Empfängt Nachrichten vom Client und sendet eine Antwort an alle Abonnenten.
 */
@Controller
public class WebSocketTestController {

    /**
     * Verarbeitet eingehende WebSocket-Nachrichten.
     * <p>
     * Die Methode empfängt Nachrichten, die an {@code /app/test} gesendet werden,
     * und gibt eine Antwort an {@code /topic/test} zurück.
     *
     * @param message Die vom Client gesendete Nachricht
     * @return Antwort des Servers inklusive Zeitstempel
     */
    @MessageMapping("/test")  // Empfängt Nachrichten an /app/test
    @SendTo("/topic/test")    // Sendet Antwort an /topic/test
    public String handleTestMessage(String message) {
        System.out.println("🔵 WebSocket Nachricht empfangen: " + message);

        String response = "Server antwortet: "
                + HtmlUtils.htmlEscape(message)
                + " (Zeit: " + java.time.LocalTime.now() + ")";

        return response;
    }
}
