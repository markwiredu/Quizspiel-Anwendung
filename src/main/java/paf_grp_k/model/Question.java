package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Repräsentiert eine Quizfrage mit mehreren Antwortmöglichkeiten.
 *
 * <p>Diese Klasse entspricht der Tabelle {@code questions} in der Datenbank.
 * Sie speichert den Fragetext, vier Antwortoptionen sowie die korrekte Antwort
 * und die zugehörige Kategorie.</p>
 */
@Data
@Entity
@Table(name = "questions")
public class Question {

    /**
     * Eindeutige ID der Frage.
     *
     * <p>Wird automatisch durch die Datenbank generiert (Auto-Increment).</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Der Text der Frage.
     *
     * <p>Kann bis zu 1000 Zeichen lang sein, um auch komplexere Fragen
     * oder Szenarien abzubilden.</p>
     */
    @Column(length = 1000)
    private String questionText;

    /**
     * Antwortoption A.
     */
    private String optionA;

    /**
     * Antwortoption B.
     */
    private String optionB;

    /**
     * Antwortoption C.
     */
    private String optionC;

    /**
     * Antwortoption D.
     */
    private String optionD;

    /**
     * Die korrekte Antwort der Frage.
     *
     * <p>Es wird erwartet, dass hier einer der Werte "A", "B", "C" oder "D"
     * gespeichert wird.</p>
     */
    private String correctAnswer;

    /**
     * Kategorie der Frage, z. B. „Sport“, „Geschichte“ oder „Informatik“.
     */
    private String category;
}
