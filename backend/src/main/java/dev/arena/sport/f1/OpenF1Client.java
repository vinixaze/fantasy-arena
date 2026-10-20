package dev.arena.sport.f1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Thin client for https://openf1.org (free tier: 3 requests/s, 30 requests/min, no key). */
@Component
public class OpenF1Client {

    private static final long MIN_INTERVAL_MS = 2_100; // stays under 30 req/min

    private final RestClient http = RestClient.builder().baseUrl("https://api.openf1.org/v1").build();
    private long lastCall = 0;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Session(@JsonProperty("session_key") long sessionKey,
                          @JsonProperty("session_name") String sessionName,
                          @JsonProperty("date_start") String dateStart,
                          @JsonProperty("location") String location,
                          @JsonProperty("country_name") String countryName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Driver(@JsonProperty("driver_number") int driverNumber,
                         @JsonProperty("full_name") String fullName,
                         @JsonProperty("name_acronym") String acronym,
                         @JsonProperty("team_name") String teamName,
                         @JsonProperty("headshot_url") String headshotUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(@JsonProperty("driver_number") int driverNumber,
                         @JsonProperty("position") Integer position,
                         @JsonProperty("dnf") Boolean dnf,
                         @JsonProperty("dns") Boolean dns,
                         @JsonProperty("dsq") Boolean dsq) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GridSlot(@JsonProperty("driver_number") int driverNumber,
                           @JsonProperty("position") Integer position) {}

    public List<Session> races(int year) {
        return fetch("/sessions?year={y}&session_name=Race", new ParameterizedTypeReference<List<Session>>() {}, year);
    }

    public List<Driver> drivers(long sessionKey) {
        return fetch("/drivers?session_key={k}", new ParameterizedTypeReference<List<Driver>>() {}, sessionKey);
    }

    public List<Result> results(long sessionKey) {
        return fetch("/session_result?session_key={k}", new ParameterizedTypeReference<List<Result>>() {}, sessionKey);
    }

    public List<GridSlot> startingGrid(long sessionKey) {
        return fetch("/starting_grid?session_key={k}", new ParameterizedTypeReference<List<GridSlot>>() {}, sessionKey);
    }

    private synchronized <T> List<T> fetch(String uri, ParameterizedTypeReference<List<T>> type, Object... vars) {
        throttle();
        try {
            List<T> body = http.get().uri(uri, vars).retrieve().body(type);
            return body == null ? List.of() : body;
        } catch (HttpClientErrorException.NotFound e) {
            return List.of(); // OpenF1 answers 404 when a query has no rows
        }
    }

    private void throttle() {
        long wait = MIN_INTERVAL_MS - (System.currentTimeMillis() - lastCall);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCall = System.currentTimeMillis();
    }
}
