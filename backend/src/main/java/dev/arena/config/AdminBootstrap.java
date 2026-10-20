package dev.arena.config;

import dev.arena.domain.User;
import dev.arena.domain.UserRole;
import dev.arena.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${arena.admin.username:}") String username,
                          @Value("${arena.admin.password:}") String password) {
        this.users = users; this.encoder = encoder; this.username = username.trim(); this.password = password;
    }

    @Override public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) return;
        User user = users.findByUsername(username).orElseGet(() -> new User(username, encoder.encode(password)));
        user.setRole(UserRole.ADMIN);
        if (!encoder.matches(password, user.getPasswordHash())) user.setPasswordHash(encoder.encode(password));
        users.save(user);
    }
}
