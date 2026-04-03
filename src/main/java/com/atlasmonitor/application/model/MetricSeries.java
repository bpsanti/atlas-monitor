package com.atlasmonitor.application.model;

import java.util.List;

public record MetricSeries(
    List<DataPoint> dataPoints,
    Peak peak
) {}
