package com.atlasmonitor.api.dto;

import com.atlasmonitor.client.resource.AtlasDataPointResource;

import java.time.Instant;
import java.util.List;

public record IopsQueryResponse(
        String processId,
        String hostname,
        String currentRole,
        String partitionName,
        String granularity,
        Instant start,
        Instant end,
        List<Instant> roleChanges,
        MetricSummary read,
        MetricSummary write,
        MetricSummary total,
        MetricSummary maxRead,
        MetricSummary maxWrite,
        MetricSummary maxTotal
) {

    public record MetricSummary(
            List<AtlasDataPointResource> dataPoints,
            PeakPoint peak
    ) {}

    public record PeakPoint(
            Instant timestamp,
            double value
    ) {}
}
