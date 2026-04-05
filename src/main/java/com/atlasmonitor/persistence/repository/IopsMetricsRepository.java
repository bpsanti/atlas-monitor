package com.atlasmonitor.persistence.repository;

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
            .toList();
    }

    public int insertAll(List<IopsMetrics> metricsList) {
        if (metricsList.isEmpty()) {
            return 0;
        }

        var documents = metricsList.stream()
            .map(m -> conversionService.convert(m, IopsMetricsDocument.class))
            .toList();
        var mergedDocuments = mergeDocuments(documents);

        try {
            var bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, IopsMetricsDocument.class);
            bulk.insert(mergedDocuments);
            return bulk.execute().getInsertedCount();
        } catch (MongoBulkWriteException e) {
            return e.getWriteResult().getInsertedCount();
        }
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
