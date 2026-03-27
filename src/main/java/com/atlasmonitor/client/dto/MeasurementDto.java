package com.atlasmonitor.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MeasurementDto(
        String name,
        String units,
        List<DataPointDto> dataPoints
) {}
