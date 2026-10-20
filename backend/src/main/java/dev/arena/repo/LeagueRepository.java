package dev.arena.repo;

import dev.arena.domain.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    Optional<League> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
