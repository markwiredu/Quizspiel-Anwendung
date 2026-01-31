package paf_grp_k.dto;

/**
 * Öffentliches Data Transfer Object (DTO) für Quizfragen.
 *
 * <p>Dieses DTO wird verwendet, um Quizfragen an Clients zu übertragen,
 * ohne die korrekte Antwort preiszugeben.</p>
 *
 * <p>Sicherheitsaspekt:</p>
 * <ul>
 *   <li>Das Feld {@code correctAnswer} ist bewusst <b>nicht</b> enthalten.</li>
 *   <li>Dadurch wird verhindert, dass Clients die Lösung aus dem Payload
 *       auslesen und cheaten können.</li>
 * </ul>
 *
 * <p>Typische Verwendung:</p>
 * <ul>
 *   <li>REST-Controller ({@code QuestionController})</li>
 *   <li>WebSocket-Events (z. B. {@code ROUND_START}, {@code GAME_START})</li>
 * </ul>
 */
public class QuestionPublicDTO {

    /**
     * Eindeutige ID der Frage.
     *
     */
    private Long id;

    /**
     * Kategorie der Frage (z. B. "SPORT", "HISTORY", "ALL").
     */
    private String category;

    /**
     * Der eigentliche Fragetext.
     */
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
     * Erstellt ein neues öffentliches DTO für eine Quizfrage.
     *
     * @param id ID der Frage
     * @param category Kategorie der Frage
     * @param questionText Text der Frage
     * @param optionA Antwortoption A
     * @param optionB Antwortoption B
     * @param optionC Antwortoption C
     * @param optionD Antwortoption D
     */
    public QuestionPublicDTO(Long id,
                             String category,
                             String questionText,
                             String optionA,
                             String optionB,
                             String optionC,
                             String optionD) {
        this.id = id;
        this.category = category;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
    }

    /**
     * @return eindeutige ID der Frage
     */
    public Long getId() {
        return id;
    }

    /**
     * @return Kategorie der Frage
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return Fragetext
     */
    public String getQuestionText() {
        return questionText;
    }

    /**
     * @return Antwortoption A
     */
    public String getOptionA() {
        return optionA;
    }

    /**
     * @return Antwortoption B
     */
    public String getOptionB() {
        return optionB;
    }

    /**
     * @return Antwortoption C
     */
    public String getOptionC() {
        return optionC;
    }

    /**
     * @return Antwortoption D
     */
    public String getOptionD() {
        return optionD;
    }
}
