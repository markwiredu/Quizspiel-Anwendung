package paf_grp_k.model;

import jakarta.persistence.*;

/**
 * Repräsentiert eine einzelne Quizfrage.
 * Enthält den Fragetext, mögliche Antwortoptionen, die korrekte Antwort
 * und eine Kategorie zur Klassifizierung.
 */
@Entity
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


    // -----------------------------------------------------
    // Getter und Setter mit JavaDoc
    // -----------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }

    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }

    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }

    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
