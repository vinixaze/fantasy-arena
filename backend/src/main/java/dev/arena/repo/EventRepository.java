package dev.arena.repo;

import dev.arena.domain.Event;
import dev.arena.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findBySportOrderByStartsAtDesc(String sport);
    Optional<Event> findBySportAndExternalId(String sport, String externalId);
    List<Event> findBySportAndStatusAndStartsAtBeforeOrderByStartsAtAsc(String sport, EventStatus status, Instant before);
}
