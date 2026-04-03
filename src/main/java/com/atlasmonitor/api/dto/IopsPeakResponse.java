package com.atlasmonitor.api.dto;

import com.atlasmonitor.api.dto.IopsQueryResponse.PeakPoint;

import java.time.Instant;

public record IopsPeakResponse(
    String processId,
    String hostname,
    String currentRole,
    String partitionName,
    String granularity,
    Instant start,
    Instant end,
    PeakPoint read,
    PeakPoint write,
    PeakPoint total,
    PeakPoint maxRead,
    PeakPoint maxWrite,
    PeakPoint maxTotal
) {}
