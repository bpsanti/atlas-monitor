package com.atlasmonitor.application.model;

import java.time.Instant;

public record IopsMetricPeak(
    Instant timestamp,
    double value
) {}
