package dev.arena.api;

import dev.arena.api.Dtos.LineupDto;
import dev.arena.api.Dtos.LineupRequest;
import dev.arena.api.Dtos.LineupSummary;
import dev.arena.service.LineupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LineupController {

    private final LineupService service;

    public LineupController(LineupService service) {
        this.service = service;
    }

    @GetMapping("/api/events/{eventId}/lineup")
    public ResponseEntity<LineupDto> mine(@AuthenticationPrincipal Long userId, @PathVariable long eventId) {
        return service.find(userId, eventId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PutMapping("/api/events/{eventId}/lineup")
    public LineupDto submit(@AuthenticationPrincipal Long userId, @PathVariable long eventId,
                            @Valid @RequestBody LineupRequest request) {
        return service.submit(userId, eventId, request.athleteIds());
    }

    @GetMapping("/api/lineups/mine")
    public List<LineupSummary> history(@AuthenticationPrincipal Long userId) {
        return service.history(userId);
    }
}
