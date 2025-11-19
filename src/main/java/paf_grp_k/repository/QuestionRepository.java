package paf_grp_k.repository;

import paf_grp_k.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

/**
 * Repository-Schnittstelle für den Zugriff auf {@link Question}-Entitäten.
 *
 * <p>Diese Schnittstelle erweitert {@link JpaRepository} und stellt damit
 * grundlegende CRUD-Operationen für Quizfragen bereit. Zusätzlich enthält sie
 * Methoden zur zufälligen Auswahl von Fragen sowie zur Filterung nach Kategorien.</p>
 */
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Wählt eine bestimmte Anzahl zufälliger Fragen aus der Datenbank aus.
     *
     * <p>Diese Methode verwendet eine native SQL-Abfrage mit <code>ORDER BY RAND()</code>,
     * um die Fragen zufällig zu mischen und anschließend eine begrenzte Anzahl zurückzugeben.</p>
     *
     * @param count die Anzahl der zufällig auszuwählenden Fragen
     * @return eine Liste zufälliger Fragen
     */
    @Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomQuestions(int count);

    /**
     * Findet alle Fragen der angegebenen Kategorie.
     *
     * @param category die Kategorie, nach der gefiltert werden soll
     * @return eine Liste aller Fragen, die der Kategorie zugeordnet sind
     */
    List<Question> findByCategory(String category);
}
