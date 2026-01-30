package paf_grp_k.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity zur Repräsentation einer einzelnen Quizfrage.
 *
 * <p>Diese Klasse speichert alle notwendigen Informationen für eine Quizfrage:</p>
 * <ul>
 *   <li>Fragetext</li>
 *   <li>vier Antwortoptionen (A–D)</li>
 *   <li>die korrekte Antwort</li>
 *   <li>eine Kategorie zur Klassifizierung</li>
 * </ul>
 *
 * <p>Die korrekte Antwort wird ausschließlich serverseitig verwendet
 * und niemals direkt an den Client übertragen.</p>
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
     * Der Text der Quizfrage.
     *
     * <p>Wird in der Datenbank in der Spalte {@code question_text} gespeichert.</p>
     */
    @Column(name = "question_text", length = 1000)
    private String questionText;

    /**
     * Antwortoption A.
     *
     * <p>Datenbank-Spalte: {@code optiona}</p>
     */
    @Column(name = "optiona")
    private String optionA;

    /**
     * Antwortoption B.
     *
     * <p>Datenbank-Spalte: {@code optionb}</p>
     */
    @Column(name = "optionb")
    private String optionB;

    /**
     * Antwortoption C.
     *
     * <p>Datenbank-Spalte: {@code optionc}</p>
     */
    @Column(name = "optionc")
    private String optionC;

    /**
     * Antwortoption D.
     *
     * <p>Datenbank-Spalte: {@code optiond}</p>
     */
    @Column(name = "optiond")
    private String optionD;

    /**
     * Die korrekte Antwort auf die Frage.
     *
     * <p>Erwartete Werte sind {@code "A"}, {@code "B"}, {@code "C"} oder {@code "D"}.</p>
     *
     * <p>Datenbank-Spalte: {@code correct_answer}</p>
     */
    @Column(name = "correct_answer")
    private String correctAnswer;

    /**
     * Kategorie der Frage (z. B. {@code "SPORT"}, {@code "HISTORY"}, {@code "ALL"}).
     *
     * <p>Wird für Filterung und kategoriebasiertes Matchmaking verwendet.</p>
     *
     * <p>Datenbank-Spalte: {@code category}</p>
     */
    @Column(name = "category")
    private String category;
}
