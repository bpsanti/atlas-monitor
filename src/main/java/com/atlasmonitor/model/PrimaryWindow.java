package com.atlasmonitor.model;

import java.time.Instant;

/**
* Represents the time window during which a specific node held the PRIMARY role.
*/
public record PrimaryWindow(
    String processId,
    String hostname,
    Instant from,
    Instant until
) {}
