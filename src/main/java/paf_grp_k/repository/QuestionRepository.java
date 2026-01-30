package paf_grp_k.repository;

import paf_grp_k.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository-Interface für den Zugriff auf {@link Question}-Entitäten.
 *
 * <p>Dieses Repository stellt verschiedene Abfragemethoden bereit,
 * um Quizfragen nach Kategorie zu filtern, zufällig auszuwählen
 * und Duplikate zu vermeiden.</p>
 *
 * <p>Es wird sowohl Spring Data JPA Query-Derivation als auch
 * benutzerdefinierte JPQL- und Native-SQL-Queries verwendet.</p>
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Liefert alle Fragen einer bestimmten Kategorie.
     *
     * <p>Spring Data JPA erzeugt diese Query automatisch
     * anhand des Methodennamens.</p>
     *
     * @param category Kategorie der Fragen
     * @return Liste aller Fragen dieser Kategorie
     */
    List<Question> findByCategory(String category);

    /**
     * Liefert eine zufällige Auswahl an Fragen aus einer bestimmten Kategorie.
     *
     * <p>Verwendet eine native SQL-Query mit {@code ORDER BY RAND()}.</p>
     *
     * <p>Hinweis:</p>
     * <p>{@code ORDER BY RAND()} kann bei sehr großen Tabellen
     * Performance-Probleme verursachen, ist hier aber für kleine
     * bis mittlere Datenmengen akzeptabel.</p>
     *
     * @param category Kategorie, aus der Fragen ausgewählt werden
     * @param limit maximale Anzahl der zurückgegebenen Fragen
     * @return Liste zufällig ausgewählter Fragen
     */
    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RAND() LIMIT :limit",
            nativeQuery = true)
    List<Question> findRandomQuestionsByCategory(@Param("category") String category,
                                                 @Param("limit") int limit);

    /**
     * Liefert eine zufällige Auswahl an Fragen aus allen Kategorien.
     *
     * <p>Verwendet eine native SQL-Query mit {@code ORDER BY RAND()}.</p>
     *
     * @param limit maximale Anzahl der zurückgegebenen Fragen
     * @return Liste zufällig ausgewählter Fragen
     */
    @Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT :limit",
            nativeQuery = true)
    List<Question> findRandomQuestions(@Param("limit") int limit);

    /**
     * Zählt die Anzahl der Fragen in einer bestimmten Kategorie.
     *
     * <p>Wird z. B. verwendet, um zu prüfen, ob genügend Fragen
     * für ein Spiel oder eine Kategorie vorhanden sind.</p>
     *
     * @param category Kategorie der Fragen
     * @return Anzahl der Fragen in dieser Kategorie
     */
    long countByCategory(String category);

    /**
     * Prüft, ob bereits eine Frage mit dem angegebenen Fragetext existiert.
     *
     * <p>Dient der Vermeidung von Duplikaten beim Anlegen neuer Fragen.</p>
     *
     * @param questionText Fragetext
     * @return {@code true}, wenn eine Frage mit diesem Text existiert
     */
    boolean existsByQuestionText(String questionText);

    /**
     * Liefert alle Fragen einer Kategorie, die in einem bestimmten Spiel
     * noch nicht verwendet wurden.
     *
     * <p>Diese Query wird verwendet, um Wiederholungen von Fragen
     * innerhalb eines Spiels zu vermeiden.</p>
     *
     * <p>Die Abfrage nutzt JPQL und referenziert die {@code Round}-Entity,
     * um bereits verwendete Fragen auszuschließen.</p>
     *
     * @param gameId ID des Spiels
     * @param category Kategorie der Fragen
     * @return Liste noch nicht verwendeter Fragen dieser Kategorie
     */
    @Query("SELECT q FROM Question q WHERE q.id NOT IN " +
            "(SELECT r.question.id FROM Round r WHERE r.game.id = :gameId) " +
            "AND q.category = :category")
    List<Question> findUnusedQuestionsByCategory(@Param("gameId") Long gameId,
                                                 @Param("category") String category);
}
