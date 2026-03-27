package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasMetricResource(
        String name,
        String units,
        List<AtlasDataPointResource> dataPoints
) {}
