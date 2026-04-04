package com.atlasmonitor.application.model;

public record SlowQueryExecution(
    Long keysExaminedCount,
    Long docsExaminedCount,
    Long docsReturnedCount,
    Boolean hasIndexCoverage,
    Boolean hasSortStage,
    SlowQueryEfficiencyRatios ratios,
    Long executionDurationMillis,
    Long responseLengthBytes,
    Long yieldsCount,
    String remoteAddress,
    Boolean isCursorExhausted
) {}
