package com.atlasmonitor.application.model;

import java.time.Instant;

public record SlowQuery(
    Instant occurredAt,
    String operationType,
    String namespace,
    long durationMillis,
    SlowQueryShape shape,
    SlowQueryExecution execution
) {}
