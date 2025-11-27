package paf_grp_k;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PafApplication {

    public static void main(String[] args) {
        SpringApplication.run(PafApplication.class, args);

        // Lombok Test
        testLombok();
    }

    private static void testLombok() {
        try {
            // Test ob Lombok funktioniert
            paf_grp_k.model.Game testGame = new paf_grp_k.model.Game();
            testGame.setStatus(paf_grp_k.model.GameStatus.FINISHED);
            System.out.println("✅ Lombok Test: setStatus() funktioniert");

            paf_grp_k.model.GameStatus status = testGame.getStatus();
            System.out.println("✅ Lombok Test: getStatus() funktioniert: " + status);

            Long id = testGame.getId();
            System.out.println("✅ Lombok Test: getId() funktioniert: " + id);

        } catch (Exception e) {
            System.err.println("❌ Lombok Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}