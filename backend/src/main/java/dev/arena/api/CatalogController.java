package dev.arena.api;

import dev.arena.api.Dtos.AthleteDto;
import dev.arena.api.Dtos.EventDto;
import dev.arena.api.Dtos.SportDto;
import dev.arena.repo.AthleteRepository;
import dev.arena.repo.EventRepository;
import dev.arena.sport.SportModule;
import dev.arena.sport.SportRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sports")
public class CatalogController {

    private final SportRegistry registry;
    private final EventRepository events;
    private final AthleteRepository athletes;

    public CatalogController(SportRegistry registry, EventRepository events, AthleteRepository athletes) {
        this.registry = registry;
        this.events = events;
        this.athletes = athletes;
    }

    @GetMapping
    public List<SportDto> sports() {
        return registry.all().stream()
                .map(m -> new SportDto(m.code(), m.displayName(), m.lineupRules().budget(), m.lineupRules().slots()))
                .toList();
    }

    @GetMapping("/{code}/events")
    public List<EventDto> events(@PathVariable String code) {
        SportModule module = module(code);
        return events.findBySportOrderByStartsAtDesc(module.code()).stream()
                .map(e -> new EventDto(e.getId(), e.getName(), e.getStartsAt(), e.getStatus().name(), e.isLocked()))
                .toList();
    }

    @GetMapping("/{code}/athletes")
    public List<AthleteDto> athletes(@PathVariable String code) {
        SportModule module = module(code);
        return athletes.findBySportOrderByKindAscPriceDesc(module.code()).stream()
                .map(a -> new AthleteDto(a.getId(), a.getName(), a.getTeam(), a.getKind(), a.getPrice(), a.getImageUrl()))
                .toList();
    }

    private SportModule module(String code) {
        return registry.find(code.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Esporte não encontrado"));
    }
}
