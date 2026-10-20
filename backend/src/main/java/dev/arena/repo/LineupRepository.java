package dev.arena.repo;

import dev.arena.domain.Lineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LineupRepository extends JpaRepository<Lineup, Long> {
    Optional<Lineup> findByUserIdAndEventId(Long userId, Long eventId);
    List<Lineup> findByEventId(Long eventId);
    List<Lineup> findByUserId(Long userId);
    List<Lineup> findByUserIdInAndEventIdIn(Collection<Long> userIds, Collection<Long> eventIds);
}
