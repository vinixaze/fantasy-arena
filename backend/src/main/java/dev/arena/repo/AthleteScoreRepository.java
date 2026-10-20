package dev.arena.repo;

import dev.arena.domain.AthleteScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AthleteScoreRepository extends JpaRepository<AthleteScore, Long> {
    List<AthleteScore> findByEventId(Long eventId);
}
