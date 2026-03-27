package com.atlasmonitor.api.dto;

import com.atlasmonitor.client.dto.DataPointDto;

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
        MetricSummary read,
        MetricSummary write,
        MetricSummary total,
        MetricSummary maxRead,
        MetricSummary maxWrite,
        MetricSummary maxTotal
) {

    public record MetricSummary(
            List<DataPointDto> dataPoints,
            PeakPoint peak
    ) {}

    public record PeakPoint(
            Instant timestamp,
            double value
    ) {}
}
