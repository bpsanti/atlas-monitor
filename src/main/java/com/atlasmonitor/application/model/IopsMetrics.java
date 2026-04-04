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
    IopsMetricSeries read,
    IopsMetricSeries write,
    IopsMetricSeries total,
    IopsMetricSeries maxRead,
    IopsMetricSeries maxWrite,
    IopsMetricSeries maxTotal
) {}
