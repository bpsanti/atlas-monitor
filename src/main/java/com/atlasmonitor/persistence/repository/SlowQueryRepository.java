package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.persistence.dao.SlowQueryDao;
import com.atlasmonitor.persistence.document.SlowQueryDocument;
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
public class SlowQueryRepository {

    private final SlowQueryDao dao;
    private final MongoTemplate mongoTemplate;
    private final ConversionService conversionService;

    public List<SlowQuery> findByDateRange(Instant from, Instant endDate, Long minDurationMillis) {
        Instant until = endDate != null ? endDate : Instant.now();
        long minDuration = minDurationMillis != null ? minDurationMillis : 0;

        return dao.findByOccurredAtBetweenAndDurationMillisGreaterThanEqualOrderByOccurredAtDesc(from, until, minDuration)
            .stream()
            .map(doc -> conversionService.convert(doc, SlowQuery.class))
            .toList();
    }

    public int insertAll(List<SlowQuery> queries, String processId) {
        if (queries.isEmpty()) {
            return 0;
        }

        var documents = queries.stream()
            .map(it -> {
                var document = conversionService.convert(it, SlowQueryDocument.class);
                document.setProcessId(processId);

                return document;
            })
            .toList();

        try {
            var bulk = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SlowQueryDocument.class);
            bulk.insert(documents);
            return bulk.execute().getInsertedCount();
        } catch (MongoBulkWriteException e) {
            return e.getWriteResult().getInsertedCount();
        }
    }
}
