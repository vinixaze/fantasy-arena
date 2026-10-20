package dev.arena.api;

import dev.arena.api.Dtos.AuthRequest;
import dev.arena.api.Dtos.AuthResponse;
import dev.arena.api.Dtos.MeDto;
import dev.arena.config.JwtService;
import dev.arena.domain.User;
import dev.arena.repo.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nome de usuário já em uso");
        }
        User user = users.save(new User(username, encoder.encode(request.password())));
        return response(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        User user = users.findByUsername(request.username().trim())
                .filter(u -> encoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos"));
        return response(user);
    }

    @GetMapping("/me")
    public MeDto me(@AuthenticationPrincipal Long userId) {
        User user = users.findById(userId).orElseThrow();
        return new MeDto(user.getId(), user.getUsername(), user.getRole().name());
    }

    private AuthResponse response(User user) {
        return new AuthResponse(jwt.issue(user.getId(), user.getUsername(), user.getRole()),
                user.getUsername(), user.getRole().name());
    }
}
