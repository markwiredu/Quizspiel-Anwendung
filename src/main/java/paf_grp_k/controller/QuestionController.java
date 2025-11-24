package paf_grp_k.controller;

import paf_grp_k.model.Question;
import paf_grp_k.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
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
    public Question getQuestionById(@PathVariable Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + id));
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
    public List<Question> getRandomQuestions(@RequestParam(defaultValue = "5") int count) {
        return questionRepository.findRandomQuestions(count);
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
    public List<Question> getQuestionsByCategory(@PathVariable String category) {
        return questionRepository.findByCategory(category);
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
    public Question createQuestion(@RequestBody Question question) {
        return questionRepository.save(question);
    }
}
