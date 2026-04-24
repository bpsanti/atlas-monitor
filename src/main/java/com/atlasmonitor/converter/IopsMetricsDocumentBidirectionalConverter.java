package com.atlasmonitor.converter;

import com.atlasmonitor.application.model.IopsDataPoint;
import com.atlasmonitor.application.model.IopsMetricPeak;
import com.atlasmonitor.application.model.IopsMetricSeries;
import com.atlasmonitor.application.model.IopsMetrics;
import com.atlasmonitor.persistence.document.IopsMetricsDocument;
import com.atlasmonitor.persistence.document.IopsMetricsDocument.DataPointEmbedded;
import com.atlasmonitor.persistence.document.IopsMetricsDocument.MetricSeriesEmbedded;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class IopsMetricsDocumentBidirectionalConverter implements BidirectionalConverter<IopsMetrics, IopsMetricsDocument> {

    @Override
    public IopsMetricsDocument convertTo(IopsMetrics source) {
        var doc = new IopsMetricsDocument();
        doc.setProcessId(source.processId());
        doc.setHostname(source.hostname());
        doc.setPartitionName(source.partitionName());
        doc.setGranularity(source.granularity());
        doc.setStart(source.start());
        doc.setEnd(source.end());
        doc.setRoleChanges(source.roleChanges());
        doc.setRead(toEmbedded(source.read()));
        doc.setWrite(toEmbedded(source.write()));
        doc.setTotal(toEmbedded(source.total()));
        doc.setMaxRead(toEmbedded(source.maxRead()));
        doc.setMaxWrite(toEmbedded(source.maxWrite()));
        doc.setMaxTotal(toEmbedded(source.maxTotal()));
        doc.setSyncedAt(Instant.now());
        return doc;
    }

    @Override
    public IopsMetrics convertFrom(IopsMetricsDocument source) {
        return new IopsMetrics(
            source.getProcessId(),
            source.getHostname(),
            "REPLICA_PRIMARY",
            source.getPartitionName(),
            source.getGranularity(),
            source.getStart(),
            source.getEnd(),
            source.getRoleChanges() != null ? source.getRoleChanges() : List.of(),
            fromEmbedded(source.getRead()),
            fromEmbedded(source.getWrite()),
            fromEmbedded(source.getTotal()),
            fromEmbedded(source.getMaxRead()),
            fromEmbedded(source.getMaxWrite()),
            fromEmbedded(source.getMaxTotal())
        );
    }

    private MetricSeriesEmbedded toEmbedded(IopsMetricSeries series) {
        if (series == null) return null;

        var dataPoints = series.dataPoints().stream()
            .map(dp -> new DataPointEmbedded(dp.timestamp(), dp.value()))
            .toList();

        var peak = series.peak();
        return new MetricSeriesEmbedded(
            dataPoints,
            peak != null ? peak.timestamp() : null,
            peak != null ? peak.value() : null
        );
    }

    private IopsMetricSeries fromEmbedded(MetricSeriesEmbedded embedded) {
        if (embedded == null) return null;

        var dataPoints = embedded.getDataPoints().stream()
            .map(dp -> new IopsDataPoint(dp.getTimestamp(), dp.getValue()))
            .toList();

        var peak = embedded.getPeakTimestamp() != null
            ? new IopsMetricPeak(embedded.getPeakTimestamp(), embedded.getPeakValue())
            : null;

        return new IopsMetricSeries(dataPoints, peak);
    }
}
