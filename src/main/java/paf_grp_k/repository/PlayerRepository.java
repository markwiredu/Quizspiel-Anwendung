package paf_grp_k.repository;

import paf_grp_k.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository-Schnittstelle für den Zugriff auf {@link Player}-Entitäten.
 *
 * <p>Diese Schnittstelle erweitert {@link JpaRepository} und bietet damit
 * grundlegende CRUD-Operationen für Spieler. Zusätzlich sind
 * zwei spezifische Methoden definiert, um Spieler über ihren
 * Benutzernamen zu finden oder die Existenz eines Benutzernamens
 * zu prüfen.</p>
 */
public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Sucht einen Spieler anhand seines Benutzernamens.
     *
     * @param username der eindeutige Benutzername
     * @return ein {@link Optional}, das den gefundenen Spieler enthält
     *         oder leer ist, wenn kein Spieler existiert
     *
     */
    Optional<Player> findByUsername(String username);



}
