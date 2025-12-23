package paf_grp_k.repository;

import paf_grp_k.model.Round;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RoundRepository extends JpaRepository<Round, Long> {

    Optional<Round> findByGameIdAndRoundNumber(Long gameId, int roundNumber);

    List<Round> findByGameIdOrderByRoundNumber(Long gameId);

    Optional<Round> findFirstByGameIdOrderByRoundNumberDesc(Long gameId);
}