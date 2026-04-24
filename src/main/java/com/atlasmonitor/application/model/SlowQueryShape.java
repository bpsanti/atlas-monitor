package com.atlasmonitor.application.model;

public record SlowQueryShape(
    String planSummary,
    String filter
) {}
