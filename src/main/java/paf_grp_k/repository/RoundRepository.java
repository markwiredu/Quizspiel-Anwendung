package paf_grp_k.repository;

import paf_grp_k.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository-Interface für den Zugriff auf {@link Round}-Entitäten.
 *
 * <p>Dieses Repository stellt Methoden zur Verfügung, um Spielrunden
 * anhand ihres zugehörigen Spiels und ihrer Rundennummer abzufragen.</p>
 *
 * <p>Es wird typischerweise von Services wie {@code GameService}
 * oder {@code RoundService} verwendet.</p>
 */
public interface RoundRepository extends JpaRepository<Round, Long> {

    /**
     * Liefert eine bestimmte Runde eines Spiels anhand der Rundennummer.
     *
     * <p>Die Kombination aus {@code gameId} und {@code roundNumber}
     * identifiziert eine Runde innerhalb eines Spiels eindeutig.</p>
     *
     * @param gameId ID des zugehörigen Spiels
     * @param roundNumber Nummer der Runde
     * @return Optional mit der gefundenen Runde oder leer, falls nicht vorhanden
     *
     */
    Optional<Round> findByGameIdAndRoundNumber(Long gameId, int roundNumber);

    /**
     * Liefert alle Runden eines Spiels in aufsteigender Reihenfolge.
     *
     * <p>Die Sortierung erfolgt anhand der {@code roundNumber}
     * und bildet damit den zeitlichen Spielverlauf ab.</p>
     *
     * @param gameId ID des zugehörigen Spiels
     * @return Liste aller Runden des Spiels, sortiert nach Rundennummer
     */
    List<Round> findByGameIdOrderByRoundNumber(Long gameId);
}
