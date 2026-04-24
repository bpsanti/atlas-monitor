package com.atlasmonitor.api.resource;

import java.time.Instant;

public record SlowQueryAnalysisResource(
    String analysis,
    String planSummary,
    Instant analyzedAt,
    boolean cached
) {}
