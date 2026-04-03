package com.atlasmonitor.application.model;

import java.time.Instant;

public record DataPoint(
    Instant timestamp,
    Double value
) {}
