package com.atlasmonitor.api.resource;

import java.time.Instant;
import java.util.List;

public record SlowQueryAnalysisResource(
    String analysis,
    String databaseAnalysis,
    List<CodeAnalysisResource> codeAnalyses,
    String planSummary,
    Instant analyzedAt,
    boolean cached
) {}
