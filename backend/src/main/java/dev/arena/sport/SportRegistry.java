package dev.arena.sport;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SportRegistry {

    private final Map<String, SportModule> modules = new LinkedHashMap<>();

    public SportRegistry(List<SportModule> all) {
        all.forEach(m -> modules.put(m.code(), m));
    }

    public Collection<SportModule> all() {
        return modules.values();
    }

    public Optional<SportModule> find(String code) {
        return Optional.ofNullable(modules.get(code));
    }
}
