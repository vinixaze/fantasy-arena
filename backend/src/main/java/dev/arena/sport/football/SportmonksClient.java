package dev.arena.sport.football;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
public class SportmonksClient {
    private final String token;
    private final RestClient http;

    public SportmonksClient(@Value("${arena.sportmonks.token:}") String token,
                            @Value("${arena.sportmonks.base-url}") String baseUrl) {
        this.token = token.trim();
        this.http = RestClient.builder().baseUrl(baseUrl).build();
    }

    public boolean configured() { return !token.isBlank(); }

    public List<JsonNode> fixtures(String start, String end) {
        if (!configured()) return List.of();
        List<JsonNode> result = new ArrayList<>();
        int page = 1;
        boolean more;
        do {
            JsonNode root = http.get().uri("/fixtures/between/{start}/{end}?api_token={token}&include=participants&per_page=100&page={page}",
                    start, end, token, page).retrieve().body(JsonNode.class);
            if (root == null || !root.path("data").isArray()) break;
            root.path("data").forEach(result::add);
            more = root.path("pagination").path("has_more").asBoolean(false);
            page++;
        } while (more && page <= 20);
        return result;
    }

    public JsonNode fixture(long id) {
        if (!configured()) return null;
        JsonNode root = http.get().uri("/fixtures/{id}?api_token={token}&include=participants;lineups.player.position;lineups.details.type",
                id, token).retrieve().body(JsonNode.class);
        return root == null ? null : root.path("data");
    }

}
