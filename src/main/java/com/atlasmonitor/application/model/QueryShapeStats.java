package com.atlasmonitor.application.model;

public record QueryShapeStats(
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
