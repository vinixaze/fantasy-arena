package dev.arena.api;

import dev.arena.api.Dtos.JoinRequest;
import dev.arena.api.Dtos.LeagueCreateRequest;
import dev.arena.api.Dtos.LeagueDto;
import dev.arena.api.Dtos.RankingRow;
import dev.arena.service.LeagueService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leagues")
public class LeagueController {

    private final LeagueService service;

    public LeagueController(LeagueService service) {
        this.service = service;
    }

    @GetMapping
    public List<LeagueDto> mine(@AuthenticationPrincipal Long userId) {
        return service.mine(userId);
    }

    @PostMapping
    public LeagueDto create(@AuthenticationPrincipal Long userId, @Valid @RequestBody LeagueCreateRequest request) {
        return service.create(userId, request.name(), request.sport());
    }

    @PostMapping("/join")
    public LeagueDto join(@AuthenticationPrincipal Long userId, @Valid @RequestBody JoinRequest request) {
        return service.join(userId, request.inviteCode());
    }

    @GetMapping("/{id}/ranking")
    public List<RankingRow> ranking(@AuthenticationPrincipal Long userId, @PathVariable long id) {
        return service.ranking(userId, id);
    }
}
