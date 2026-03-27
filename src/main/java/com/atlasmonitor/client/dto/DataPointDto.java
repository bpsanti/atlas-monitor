package com.atlasmonitor.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DataPointDto(
        Instant timestamp,
        Double value        // nullable — Atlas sends null for gaps
) {}
