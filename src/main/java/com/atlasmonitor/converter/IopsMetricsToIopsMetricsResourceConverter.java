package com.atlasmonitor.converter;

import com.atlasmonitor.api.resource.IopsMetricsResource;
import com.atlasmonitor.api.resource.IopsMetricsResource.DataPointResource;
import com.atlasmonitor.api.resource.IopsMetricsResource.MetricSummaryResource;
import com.atlasmonitor.api.resource.IopsMetricsResource.PeakResource;
import com.atlasmonitor.application.model.IopsMetrics;
import com.atlasmonitor.application.model.IopsMetricSeries;
import com.atlasmonitor.application.model.IopsMetricPeak;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IopsMetricsToIopsMetricsResourceConverter implements Converter<IopsMetrics, IopsMetricsResource> {

    @Override
    public IopsMetricsResource convert(IopsMetrics source) {
        return new IopsMetricsResource(
            source.processId(),
            source.hostname(),
            source.currentRole(),
            source.partitionName(),
            source.granularity(),
            source.start(),
            source.end(),
            source.roleChanges(),
            toSummaryResource(source.read()),
            toSummaryResource(source.write()),
            toSummaryResource(source.total()),
            toSummaryResource(source.maxRead()),
            toSummaryResource(source.maxWrite()),
            toSummaryResource(source.maxTotal())
        );
    }

    private MetricSummaryResource toSummaryResource(IopsMetricSeries series) {
        if (series == null) {
            return null;
        }
        List<DataPointResource> dataPoints = series.dataPoints().stream()
            .map(dp -> new DataPointResource(dp.timestamp(), dp.value()))
            .toList();
        return new MetricSummaryResource(dataPoints, toPeakResource(series.peak()));
    }

    private PeakResource toPeakResource(IopsMetricPeak peak) {
        if (peak == null) {
            return null;
        }
        return new PeakResource(peak.timestamp(), peak.value());
    }
}
