package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasDataPointResource(
        Instant timestamp,
        Double value        // nullable — Atlas sends null for gaps
) {}
