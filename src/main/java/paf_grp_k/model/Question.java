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

    // DB: question_text
    @Column(name = "question_text", length = 1000)
    private String questionText;

    // DB: optiona / optionb / optionc / optiond (alles klein!)
    @Column(name = "optiona")
    private String optionA;

    @Column(name = "optionb")
    private String optionB;

    @Column(name = "optionc")
    private String optionC;

    @Column(name = "optiond")
    private String optionD;

    // DB: correct_answer
    @Column(name = "correct_answer")
    private String correctAnswer;

    // DB: category
    @Column(name = "category")
    private String category;
}
