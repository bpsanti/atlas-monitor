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
import java.util.List;

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

        try {
            var bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, IopsMetricsDocument.class);
            bulk.insert(documents);
            return bulk.execute().getInsertedCount();
        } catch (MongoBulkWriteException e) {
            return e.getWriteResult().getInsertedCount();
        }
    }
}
