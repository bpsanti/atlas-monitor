package com.atlasmonitor.application.model;

import java.time.Instant;

public record IopsDataPoint(
    Instant timestamp,
    Double value
) {}
