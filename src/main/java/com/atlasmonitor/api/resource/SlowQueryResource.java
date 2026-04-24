package com.atlasmonitor.api.resource;

import java.time.Instant;

public record SlowQueryResource(
    Instant occurredAt,
    String operationType,
    String namespace,
    long durationMillis,
    String planSummary,
    String filter,
    QueryExecutionResource execution
) {

    public record QueryExecutionResource(
        Long keysExaminedCount,
        Long docsExaminedCount,
        Long docsReturnedCount,
        Boolean hasIndexCoverage,
        Boolean hasSortStage,
        Double docsExaminedToReturnedRatio,
        Double keysExaminedToReturnedRatio,
        Long executionDurationMillis,
        Long responseLengthBytes,
        Long yieldsCount,
        String remoteAddress,
        Boolean isCursorExhausted
    ) {}
}
