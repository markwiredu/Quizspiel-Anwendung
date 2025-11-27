package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Repräsentiert eine einzelne Quizfrage.
 * Enthält den Fragetext, mögliche Antwortoptionen, die korrekte Antwort
 * und eine Kategorie zur Klassifizierung.
 */
@Entity
@Getter
@Setter
@Table(name = "questions")
public class Question {

    /**
     * Eindeutige ID der Frage (Primärschlüssel).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Text der Frage.
     * Kann bis zu 1000 Zeichen lang sein.
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
     * Korrekte Antwort. Erwartete Werte: "A", "B", "C" oder "D".
     */
    private String correctAnswer;

    /**
     * Kategorie der Frage, z. B. "Sport", "Geschichte".
     */
    private String category;
}