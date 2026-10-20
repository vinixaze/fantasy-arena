package dev.arena.repo;

import dev.arena.domain.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findBySportOrderByKindAscPriceDesc(String sport);
    Optional<Athlete> findBySportAndExternalId(String sport, String externalId);
}
