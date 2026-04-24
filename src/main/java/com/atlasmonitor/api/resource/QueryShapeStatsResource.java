package com.atlasmonitor.api.resource;

public record QueryShapeStatsResource(
    String shapeHash,
    String namespace,
    String planSummary,
    String normalizedFilter,
    long queryCount,
    long totalDurationMillis,
    double avgDurationMillis,
    long maxDurationMillis,
    long minDurationMillis
) {}
