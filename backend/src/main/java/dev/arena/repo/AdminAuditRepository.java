package dev.arena.repo;

import dev.arena.domain.AdminAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminAuditRepository extends JpaRepository<AdminAudit, Long> {
    List<AdminAudit> findTop30ByOrderByCreatedAtDesc();
}
