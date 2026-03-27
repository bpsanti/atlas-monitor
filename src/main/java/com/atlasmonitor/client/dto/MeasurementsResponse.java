package com.atlasmonitor.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeasurementsResponse(
        String groupId,
        String processId,
        String partitionName,
        String granularity,
        Instant start,
        Instant end,
        List<MeasurementDto> measurements
) {}
