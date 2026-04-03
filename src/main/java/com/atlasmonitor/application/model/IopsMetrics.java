package com.atlasmonitor.application.model;

import java.time.Instant;
import java.util.List;

public record IopsMetrics(
    String processId,
    String hostname,
    String currentRole,
    String partitionName,
    String granularity,
    Instant start,
    Instant end,
    List<Instant> roleChanges,
    MetricSeries read,
    MetricSeries write,
    MetricSeries total,
    MetricSeries maxRead,
    MetricSeries maxWrite,
    MetricSeries maxTotal
) {}
