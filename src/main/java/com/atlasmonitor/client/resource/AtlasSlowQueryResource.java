package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasSlowQueryResource(
    String line,
    String namespace,
    String opType,
    String replicaState,
    AtlasSlowQueryMetricsResource metrics
) {}
