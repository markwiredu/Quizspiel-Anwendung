package paf_grp_k.repository;

import paf_grp_k.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Finde Fragen nach Kategorie
     */
    List<Question> findByCategory(String category);

    /**
     * Finde zufällige Fragen (limitierte Anzahl)
     */
    @Query(value = "SELECT * FROM questions WHERE category = :category ORDER BY RAND() LIMIT :limit",
            nativeQuery = true)
    List<Question> findRandomQuestionsByCategory(@Param("category") String category,
                                                 @Param("limit") int limit);

    /**
     * Finde zufällige Fragen (alle Kategorien)
     */
    @Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT :limit",
            nativeQuery = true)
    List<Question> findRandomQuestions(@Param("limit") int limit);

    /**
     * Zähle Fragen in einer Kategorie
     */
    long countByCategory(String category);

    /**
     * Prüfe ob Frage mit Text existiert (Vermeidung von Duplikaten)
     */
    boolean existsByQuestionText(String questionText);

    /**
     * Finde Fragen die noch nicht in einem Spiel verwendet wurden
     */
    @Query("SELECT q FROM Question q WHERE q.id NOT IN " +
            "(SELECT r.question.id FROM Round r WHERE r.game.id = :gameId) " +
            "AND q.category = :category")
    List<Question> findUnusedQuestionsByCategory(@Param("gameId") Long gameId,
                                                 @Param("category") String category);
}