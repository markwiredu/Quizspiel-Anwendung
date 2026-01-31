package paf_grp_k.controller;

import paf_grp_k.dto.QuestionPublicDTO;
import paf_grp_k.model.Question;
import paf_grp_k.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST-Controller zur Verwaltung von Quizfragen.
 *
 * <p>Dieser Controller stellt Endpunkte bereit, um:</p>
 * <ul>
 *   <li>alle Fragen abzurufen</li>
 *   <li>eine Frage per ID abzurufen</li>
 *   <li>zufällige Fragen auszuwählen</li>
 *   <li>Fragen nach Kategorie zu filtern</li>
 *   <li>neue Fragen anzulegen</li>
 * </ul>
 *
 * <p>Sicherheits-Hinweis:</p>
 * <p>Für Client-Ausgaben wird {@link QuestionPublicDTO} verwendet, damit keine
 * korrekte Antwort übertragen wird (Cheating vermeiden).</p>
 *
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    /**
     * Repository für den Zugriff auf {@link Question}-Entitäten.
     */
    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Liefert alle gespeicherten Fragen.
     *
     * <p>HTTP GET: {@code /api/questions}</p>
     *
     * <p>Die Rückgabe erfolgt als {@link QuestionPublicDTO}, damit die korrekte Antwort
     * nicht an den Client gesendet wird.</p>
     *
     * @return Liste aller Fragen als Public-DTO
     */
    @GetMapping
    public List<QuestionPublicDTO> getAllQuestions() {
        return questionRepository.findAll()
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    /**
     * Liefert eine einzelne Frage anhand ihrer ID.
     *
     * <p>HTTP GET: {@code /api/questions/{id}}</p>
     *
     * @param id eindeutige ID der Frage
     * @return gefundene Frage als {@link QuestionPublicDTO}
     * @throws RuntimeException wenn keine Frage mit dieser ID existiert
     */
    @GetMapping("/{id}")
    public QuestionPublicDTO getQuestionById(@PathVariable Long id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return toPublicDto(q);
    }

    /**
     * Liefert eine zufällige Auswahl an Fragen.
     *
     * <p>HTTP GET: {@code /api/questions/random?count=n}</p>
     *
     * <p>Wenn {@code count} nicht gesetzt ist, werden standardmäßig 5 Fragen geliefert.</p>
     *
     * @param count Anzahl der zufällig auszuwählenden Fragen (Standard: 5)
     * @return Liste zufällig ausgewählter Fragen als {@link QuestionPublicDTO}
     */
    @GetMapping("/random")
    public List<QuestionPublicDTO> getRandomQuestions(@RequestParam(defaultValue = "5") int count) {
        return questionRepository.findRandomQuestions(count)
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    /**
     * Liefert alle Fragen einer bestimmten Kategorie.
     *
     * <p>HTTP GET: {@code /api/questions/category/{category}}</p>
     *
     * @param category Kategorie, nach der gefiltert werden soll
     * @return Liste aller Fragen dieser Kategorie als {@link QuestionPublicDTO}
     */
    @GetMapping("/category/{category}")
    public List<QuestionPublicDTO> getQuestionsByCategory(@PathVariable String category) {
        return questionRepository.findByCategory(category)
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    /**
     * Erstellt eine neue Quizfrage und speichert sie in der Datenbank.
     *
     * <p>HTTP POST: {@code /api/questions}</p>
     *
     * <p>Validierung:</p>
     * <ul>
     *   <li>Fragentext darf nicht doppelt vorhanden sein (Duplikatprüfung)</li>
     *   <li>{@code correctAnswer} muss einer der Werte A/B/C/D sein</li>
     * </ul>
     *
     * <p>Die Antwort wird gespeichert, aber bei der Rückgabe an den Client wird nur
     * ein {@link QuestionPublicDTO} geliefert (ohne Lösung).</p>
     *
     * @param question neue Frage (Entity), die gespeichert werden soll
     * @return gespeicherte Frage als {@link QuestionPublicDTO}
     * @throws RuntimeException bei Duplikat oder ungültiger {@code correctAnswer}
     */
    @PostMapping
    public QuestionPublicDTO createQuestion(@RequestBody Question question) {

        if (questionRepository.existsByQuestionText(question.getQuestionText())) {
            throw new RuntimeException("Question already exists: " + question.getQuestionText());
        }

        // Basic validation: correctAnswer muss A/B/C/D sein
        String ca = question.getCorrectAnswer();
        if (ca == null || !(ca.equalsIgnoreCase("A")
                || ca.equalsIgnoreCase("B")
                || ca.equalsIgnoreCase("C")
                || ca.equalsIgnoreCase("D"))) {
            throw new RuntimeException("correctAnswer must be A, B, C or D");
        }

        Question saved = questionRepository.save(question);
        return toPublicDto(saved);
    }

    /**
     * Konvertiert eine {@link Question}-Entity in ein {@link QuestionPublicDTO}.
     *
     * <p>Wichtig: Das DTO enthält keine korrekte Antwort, um das Cheating-Risiko zu reduzieren.</p>
     *
     * @param q Question-Entity
     * @return Public-DTO zur sicheren Ausgabe an Clients
     */
    private QuestionPublicDTO toPublicDto(Question q) {
        return new QuestionPublicDTO(
                q.getId(),
                q.getCategory(),
                q.getQuestionText(),
                q.getOptionA(),
                q.getOptionB(),
                q.getOptionC(),
                q.getOptionD()
        );
    }
}
