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
    Peak read,
    Peak write,
    Peak total,
    Peak maxRead,
    Peak maxWrite,
    Peak maxTotal
) {}
