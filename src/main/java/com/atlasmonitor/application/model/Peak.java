package com.atlasmonitor.application.model;

import java.time.Instant;

public record Peak(
    Instant timestamp,
    double value
) {}
