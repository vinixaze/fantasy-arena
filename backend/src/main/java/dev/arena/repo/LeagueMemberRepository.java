package dev.arena.repo;

import dev.arena.domain.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {
    List<LeagueMember> findByUserId(Long userId);
    List<LeagueMember> findByLeagueId(Long leagueId);
    boolean existsByLeagueIdAndUserId(Long leagueId, Long userId);
}
