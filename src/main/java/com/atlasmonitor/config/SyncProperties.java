package com.atlasmonitor.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "atlas.sync")
public record SyncProperties(
    @NotNull Duration interval,
    @NotNull Duration syncWindowOverlap,
    @NotNull Duration slowQueryMinDuration,
    @NotNull Duration slowQueryBatchWindow,
    @NotNull Duration slowQueryInitialLookback,
    @NotNull Duration primaryWindowInitialLookback
) {}
