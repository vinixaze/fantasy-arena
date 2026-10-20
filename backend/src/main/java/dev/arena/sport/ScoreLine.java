package dev.arena.sport;

import java.util.Map;

public record ScoreLine(double points, Map<String, Double> breakdown) {}
