package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasMetricWrapperResource(
        String groupId,
        String processId,
        String partitionName,
        String granularity,
        Instant start,
        Instant end,
        List<AtlasMetricResource> measurements
) {}
