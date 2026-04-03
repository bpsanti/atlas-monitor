package com.atlasmonitor.application.model;

import java.time.Instant;

public record SlowQuery(
    Instant date,
    String type,
    String namespace,
    long durationMillis,
    String planSummary,
    Long keysExamined,
    Long docsExamined,
    Long nreturned,
    Double docsExaminedReturnedRatio,
    Double keysExaminedReturnedRatio,
    Boolean hasIndexCoverage,
    Boolean hasSort,
    Long operationExecutionTime,
    Long responseLength,
    Long numYields,
    String remote,
    Boolean cursorExhausted,
    String filter
) {}
