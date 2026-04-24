package com.atlasmonitor.api.resource;

import java.time.Instant;
import java.util.List;

public record IopsMetricsResource(
    String processId,
    String hostname,
    String currentRole,
    String partitionName,
    String granularity,
    Instant start,
    Instant end,
    List<Instant> roleChanges,
    MetricSummaryResource read,
    MetricSummaryResource write,
    MetricSummaryResource total,
    MetricSummaryResource maxRead,
    MetricSummaryResource maxWrite,
    MetricSummaryResource maxTotal
) {

    public record MetricSummaryResource(
        List<DataPointResource> dataPoints,
        PeakResource peak
    ) {}

    public record DataPointResource(
        Instant timestamp,
        Double value
    ) {}

    public record PeakResource(
        Instant timestamp,
        double value
    ) {}
}
