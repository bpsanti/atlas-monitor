package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.model.IopsDataPoint;
import com.atlasmonitor.application.model.IopsMetricPeak;
import com.atlasmonitor.application.model.IopsMetricSeries;
import com.atlasmonitor.application.model.IopsMetrics;
import com.atlasmonitor.persistence.dao.IopsMetricsDao;
import com.atlasmonitor.persistence.document.IopsMetricsDocument;
import com.mongodb.MongoBulkWriteException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class IopsMetricsRepository {

    private final IopsMetricsDao dao;
    private final MongoTemplate mongoTemplate;
    private final ConversionService conversionService;

    public List<IopsMetrics> findByDateRange(Instant start, Instant end) {
        return dao.findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(end, start)
            .stream()
            .map(doc -> conversionService.convert(doc, IopsMetrics.class))
            .map(metrics -> trimToRange(metrics, start, end))
            .toList();
    }

    private IopsMetrics trimToRange(IopsMetrics metrics, Instant start, Instant end) {
        return new IopsMetrics(
            metrics.processId(),
            metrics.hostname(),
            metrics.currentRole(),
            metrics.partitionName(),
            metrics.granularity(),
            start,
            end,
            metrics.roleChanges(),
            trimSeries(metrics.read(), start, end),
            trimSeries(metrics.write(), start, end),
            trimSeries(metrics.total(), start, end),
            trimSeries(metrics.maxRead(), start, end),
            trimSeries(metrics.maxWrite(), start, end),
            trimSeries(metrics.maxTotal(), start, end)
        );
    }

    private IopsMetricSeries trimSeries(IopsMetricSeries series, Instant start, Instant end) {
        if (series == null) return null;

        var filtered = series.dataPoints().stream()
            .filter(dp -> dp.timestamp() != null
                && !dp.timestamp().isBefore(start)
                && !dp.timestamp().isAfter(end))
            .toList();

        var peak = filtered.stream()
            .filter(dp -> dp.value() != null)
            .max(java.util.Comparator.comparingDouble(IopsDataPoint::value))
            .map(dp -> new IopsMetricPeak(dp.timestamp(), dp.value()))
            .orElse(null);

        return new IopsMetricSeries(filtered, peak);
    }

    public int insertAll(List<IopsMetrics> metricsList) {
        if (metricsList.isEmpty()) {
            return 0;
        }

        var documents = metricsList.stream()
            .map(m -> conversionService.convert(m, IopsMetricsDocument.class))
            .toList();
        var mergedDocuments = mergeDocuments(documents);

        return dao.saveAll(mergedDocuments).size();
    }

    private List<IopsMetricsDocument> mergeDocuments(List<IopsMetricsDocument> documents) {
        var lastSavedDocument = dao.findTopByOrderByEndDesc().orElse(null);
        if (lastSavedDocument == null) {
            return documents;
        }

        var finalDocuments = new ArrayList<IopsMetricsDocument>();
        for (var document : documents) {
            if (Objects.equals(lastSavedDocument.getProcessId(), document.getProcessId())) {
                lastSavedDocument.getRead().getDataPoints().addAll(document.getRead().getDataPoints());
                lastSavedDocument.getWrite().getDataPoints().addAll(document.getWrite().getDataPoints());
                lastSavedDocument.getTotal().getDataPoints().addAll(document.getTotal().getDataPoints());
                lastSavedDocument.getMaxRead().getDataPoints().addAll(document.getMaxRead().getDataPoints());
                lastSavedDocument.getMaxWrite().getDataPoints().addAll(document.getMaxWrite().getDataPoints());
                lastSavedDocument.getMaxTotal().getDataPoints().addAll(document.getMaxTotal().getDataPoints());
                lastSavedDocument.setEnd(document.getEnd());
                lastSavedDocument.setSyncedAt(document.getSyncedAt());
                finalDocuments.add(lastSavedDocument);

                continue;
            }

            finalDocuments.add(document);
        }

        return finalDocuments;
    }
}
