package com.atlasmonitor.application.model;

import java.time.Instant;

public record IopsPeak(
    String processId,
    String hostname,
    String currentRole,
    String partitionName,
    String granularity,
    Instant start,
    Instant end,
    IopsMetricPeak read,
    IopsMetricPeak write,
    IopsMetricPeak total,
    IopsMetricPeak maxRead,
    IopsMetricPeak maxWrite,
    IopsMetricPeak maxTotal
) {}
