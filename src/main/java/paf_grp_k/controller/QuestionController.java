package paf_grp_k.controller;

import paf_grp_k.model.Question;
import paf_grp_k.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import paf_grp_k.dto.QuestionPublicDTO;
import java.util.stream.Collectors;


import java.util.List;

/**
 * REST-Controller für die Verwaltung von Quizfragen.
 *
 * <p>Dieser Controller stellt Endpunkte bereit, um Fragen abzurufen,
 * zu filtern, zufällig auszuwählen und neue Fragen anzulegen.</p>
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    /**
     * Repository für den Zugriff auf {@link Question}-Daten.
     */
    @Autowired
    private QuestionRepository questionRepository;

    /**
     * Gibt alle gespeicherten Fragen zurück.
     *
     * <p>HTTP GET: {@code /api/questions}</p>
     *
     * @return Liste aller Fragen in der Datenbank
     */
    @GetMapping
    public List<QuestionPublicDTO> getAllQuestions() {
        return questionRepository.findAll()
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }


    /**
     * Gibt eine einzelne Frage anhand ihrer ID zurück.
     *
     * <p>HTTP GET: {@code /api/questions/{id}}</p>
     *
     * @param id die eindeutige ID der Frage
     * @return die gefundene Frage
     * @throws RuntimeException wenn keine Frage mit dieser ID existiert
     */
    @GetMapping("/{id}")
    public QuestionPublicDTO getQuestionById(@PathVariable Long id) {
        Question q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
        return toPublicDto(q);
    }

    /**
     * Gibt eine zufällige Auswahl an Fragen zurück.
     *
     * <p>HTTP GET: {@code /api/questions/random?count=n}</p>
     *
     * @param count Anzahl der zufällig auszuwählenden Fragen (Standard: 5)
     * @return Liste zufällig ausgewählter Fragen
     */
    @GetMapping("/random")
    public List<QuestionPublicDTO> getRandomQuestions(@RequestParam(defaultValue = "5") int count) {
        return questionRepository.findRandomQuestions(count)
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    /**
     * Gibt alle Fragen einer bestimmten Kategorie zurück.
     *
     * <p>HTTP GET: {@code /api/questions/category/{category}}</p>
     *
     * @param category die Kategorie, nach der gefiltert werden soll
     * @return Liste aller Fragen in dieser Kategorie
     */
    @GetMapping("/category/{category}")
    public List<QuestionPublicDTO> getQuestionsByCategory(@PathVariable String category) {
        return questionRepository.findByCategory(category)
                .stream()
                .map(this::toPublicDto)
                .collect(Collectors.toList());
    }

    /**
     * Erstellt eine neue Frage.
     *
     * <p>HTTP POST: {@code /api/questions}</p>
     *
     * @param question die neue Frage, die gespeichert werden soll
     * @return die gespeicherte Frage
     */
    @PostMapping
    public QuestionPublicDTO createQuestion(@RequestBody Question question) {

        if (questionRepository.existsByQuestionText(question.getQuestionText())) {
            throw new RuntimeException("Question already exists: " + question.getQuestionText());
        }

        // Optional: basic validation
        String ca = question.getCorrectAnswer();
        if (ca == null || !(ca.equalsIgnoreCase("A") || ca.equalsIgnoreCase("B") || ca.equalsIgnoreCase("C") || ca.equalsIgnoreCase("D"))) {
            throw new RuntimeException("correctAnswer must be A, B, C or D");
        }

        Question saved = questionRepository.save(question);
        return toPublicDto(saved);
    }


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
