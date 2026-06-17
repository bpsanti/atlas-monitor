package com.atlasmonitor.application.model;

import java.time.Instant;
import java.util.List;

public record SlowQueryAnalysis(
    String shapeHash,
    String namespace,
    String planSummary,
    String analysis,
    String databaseAnalysis,
    List<CodeAnalysis> codeAnalyses,
    Instant analyzedAt,
    boolean cached
) {}
