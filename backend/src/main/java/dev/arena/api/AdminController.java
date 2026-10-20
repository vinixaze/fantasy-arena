package dev.arena.api;

import dev.arena.api.Dtos.*;
import dev.arena.domain.*;
import dev.arena.repo.*;
import dev.arena.service.ScoringService;
import dev.arena.service.SyncService;
import dev.arena.sport.ScoreLine;
import dev.arena.sport.football.SportmonksClient;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final EventRepository events;
    private final AthleteRepository athletes;
    private final LeagueRepository leagues;
    private final AdminAuditRepository audits;
    private final ScoringService scoring;
    private final SyncService sync;
    private final SportmonksClient sportmonks;

    public AdminController(UserRepository users, EventRepository events, AthleteRepository athletes,
                           LeagueRepository leagues, AdminAuditRepository audits, ScoringService scoring,
                           SyncService sync, SportmonksClient sportmonks) {
        this.users = users; this.events = events; this.athletes = athletes; this.leagues = leagues;
        this.audits = audits; this.scoring = scoring; this.sync = sync; this.sportmonks = sportmonks;
    }

    @GetMapping("/dashboard")
    public AdminDashboardDto dashboard() {
        return new AdminDashboardDto(users.count(), events.count(), athletes.count(), leagues.count(), sportmonks.configured());
    }

    @GetMapping("/users")
    public List<AdminUserDto> users() {
        return users.findAll().stream().map(u -> new AdminUserDto(u.getId(), u.getUsername(), u.getRole().name())).toList();
    }

    @PatchMapping("/users/{id}/role") @Transactional
    public AdminUserDto role(@AuthenticationPrincipal Long adminId, @PathVariable long id, @Valid @RequestBody RoleRequest body) {
        UserRole role;
        try { role = UserRole.valueOf(body.role().toUpperCase()); }
        catch (IllegalArgumentException e) { throw bad("Papel inválido"); }
        User user = users.findById(id).orElseThrow(() -> notFound("Usuário não encontrado"));
        if (user.getRole() == UserRole.ADMIN && role != UserRole.ADMIN &&
                users.findAll().stream().filter(u -> u.getRole() == UserRole.ADMIN).count() <= 1) {
            throw bad("Não é possível remover o último administrador");
        }
        user.setRole(role); users.save(user); audit(adminId, "CHANGE_ROLE", "USER", String.valueOf(id), role.name());
        return new AdminUserDto(user.getId(), user.getUsername(), user.getRole().name());
    }

    @GetMapping("/ufc/fighters")
    public List<AthleteDto> fighters() { return athleteDtos("UFC"); }

    @PostMapping("/ufc/fighters") @Transactional
    public AthleteDto createFighter(@AuthenticationPrincipal Long adminId, @Valid @RequestBody AdminAthleteRequest body) {
        Athlete a = new Athlete("UFC", "manual-" + UUID.randomUUID(), body.name().trim(), body.team(), "FIGHTER",
                blankToNull(body.imageUrl()), positivePrice(body.price()));
        athletes.save(a); audit(adminId, "CREATE", "UFC_FIGHTER", String.valueOf(a.getId()), a.getName());
        return dto(a);
    }

    @PutMapping("/ufc/fighters/{id}") @Transactional
    public AthleteDto updateFighter(@AuthenticationPrincipal Long adminId, @PathVariable long id,
                                    @Valid @RequestBody AdminAthleteRequest body) {
        Athlete a = ufcAthlete(id); a.setName(body.name().trim()); a.setTeam(blankToNull(body.team()));
        a.setImageUrl(blankToNull(body.imageUrl())); a.setPrice(positivePrice(body.price())); athletes.save(a);
        audit(adminId, "UPDATE", "UFC_FIGHTER", String.valueOf(id), a.getName()); return dto(a);
    }

    @GetMapping("/ufc/events")
    public List<EventDto> ufcEvents() { return eventDtos("UFC"); }

    @PostMapping("/ufc/events") @Transactional
    public EventDto createUfcEvent(@AuthenticationPrincipal Long adminId, @Valid @RequestBody AdminEventRequest body) {
        Event e = events.save(new Event("UFC", "manual-" + UUID.randomUUID(), body.name().trim(), body.startsAt()));
        audit(adminId, "CREATE", "UFC_EVENT", String.valueOf(e.getId()), e.getName()); return dto(e);
    }

    @PutMapping("/ufc/events/{id}") @Transactional
    public EventDto updateUfcEvent(@AuthenticationPrincipal Long adminId, @PathVariable long id,
                                   @Valid @RequestBody AdminEventRequest body) {
        Event e = ufcEvent(id); if (e.getStatus() == EventStatus.SCORED) throw bad("Evento pontuado não pode ser editado");
        e.setName(body.name().trim()); e.setStartsAt(body.startsAt()); events.save(e);
        audit(adminId, "UPDATE", "UFC_EVENT", String.valueOf(id), e.getName()); return dto(e);
    }

    @PostMapping("/ufc/events/{id}/score") @Transactional
    public EventDto scoreUfc(@AuthenticationPrincipal Long adminId, @PathVariable long id,
                             @Valid @RequestBody AdminScoreRequest body) {
        Event event = ufcEvent(id);
        if (event.getStatus() == EventStatus.SCORED) throw bad("Este evento já foi pontuado");
        Map<String, ScoreLine> lines = new LinkedHashMap<>();
        body.points().forEach((athleteId, points) -> {
            Athlete a = ufcAthlete(athleteId);
            lines.put(a.getExternalId(), new ScoreLine(points, Map.of("manual", points)));
        });
        if (lines.isEmpty()) throw bad("Informe a pontuação de pelo menos um lutador");
        scoring.apply(id, lines); audit(adminId, "PUBLISH_SCORE", "UFC_EVENT", String.valueOf(id), lines.size() + " lutadores");
        return dto(events.findById(id).orElseThrow());
    }

    @PostMapping("/sync")
    public Map<String,String> sync(@AuthenticationPrincipal Long adminId) {
        sync.syncAll(); audit(adminId, "SYNC", "SPORTS", null, "Sincronização manual");
        return Map.of("message", "Sincronização concluída");
    }

    @GetMapping("/audit")
    public List<AuditDto> audit() {
        return audits.findTop30ByOrderByCreatedAtDesc().stream()
                .map(a -> new AuditDto(a.getAction(), a.getEntityType(), a.getEntityId(), a.getDetails(), a.getCreatedAt())).toList();
    }

    private List<AthleteDto> athleteDtos(String sport) { return athletes.findBySportOrderByKindAscPriceDesc(sport).stream().map(this::dto).toList(); }
    private List<EventDto> eventDtos(String sport) { return events.findBySportOrderByStartsAtDesc(sport).stream().map(this::dto).toList(); }
    private Athlete ufcAthlete(long id) { Athlete a = athletes.findById(id).orElseThrow(() -> notFound("Lutador não encontrado")); if (!a.getSport().equals("UFC")) throw bad("Atleta não pertence ao UFC"); return a; }
    private Event ufcEvent(long id) { Event e = events.findById(id).orElseThrow(() -> notFound("Evento não encontrado")); if (!e.getSport().equals("UFC")) throw bad("Evento não pertence ao UFC"); return e; }
    private AthleteDto dto(Athlete a) { return new AthleteDto(a.getId(), a.getName(), a.getTeam(), a.getKind(), a.getPrice(), a.getImageUrl()); }
    private EventDto dto(Event e) { return new EventDto(e.getId(), e.getName(), e.getStartsAt(), e.getStatus().name(), e.isLocked()); }
    private double positivePrice(double value) { if (value <= 0) throw bad("O preço deve ser maior que zero"); return value; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void audit(Long admin, String action, String type, String id, String details) { audits.save(new AdminAudit(admin, action, type, id, details)); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
