package dev.arena.sport;

import java.time.Instant;

public record EventData(String externalId, String name, Instant startsAt) {}
