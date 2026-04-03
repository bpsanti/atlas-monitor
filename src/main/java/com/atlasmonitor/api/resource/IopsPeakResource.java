package com.atlasmonitor.api.resource;

import com.atlasmonitor.api.resource.IopsMetricsResource.PeakResource;

import java.time.Instant;

public record IopsPeakResource(
    String processId,
    String hostname,
    String currentRole,
    String partitionName,
    String granularity,
    Instant start,
    Instant end,
    PeakResource read,
    PeakResource write,
    PeakResource total,
    PeakResource maxRead,
    PeakResource maxWrite,
    PeakResource maxTotal
) {}
