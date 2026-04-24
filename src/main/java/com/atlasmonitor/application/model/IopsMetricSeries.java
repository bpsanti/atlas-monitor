package com.atlasmonitor.application.model;

import java.util.List;

public record IopsMetricSeries(
    List<IopsDataPoint> dataPoints,
    IopsMetricPeak peak
) {}
