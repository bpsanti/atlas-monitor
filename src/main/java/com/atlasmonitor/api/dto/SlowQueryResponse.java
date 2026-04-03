package com.atlasmonitor.api.dto;

import java.time.Instant;

public record SlowQueryResponse(
        Instant date,
        String type,
        String namespace,
        long durationMillis,
        String planSummary,
        Long keysExamined,
        Long docsExamined,
        Long nreturned,
        String remote,
        Boolean cursorExhausted,
        String filter
) {}
