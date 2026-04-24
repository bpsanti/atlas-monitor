package com.atlasmonitor.application.model;

import java.time.Instant;

public record SlowQueryAnalysis(
    String shapeHash,
    String namespace,
    String planSummary,
    String analysis,
    Instant analyzedAt,
    boolean cached
) {}
