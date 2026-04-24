package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasSlowQueryMetricsResource(
    Long docsExamined,
    Long docsReturned,
    Double docsExaminedReturnedRatio,
    Long keysExamined,
    Double keysExaminedReturnedRatio,
    Boolean hasIndexCoverage,
    Boolean hasSort,
    Long operationExecutionTime,
    Long responseLength,
    Long numYields
) {}
