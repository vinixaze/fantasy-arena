package dev.arena.sport;

import java.util.Map;

/** Budget and number of slots per athlete kind, e.g. {DRIVER=2, TEAM=1}. */
public record LineupRules(double budget, Map<String, Integer> slots) {}
