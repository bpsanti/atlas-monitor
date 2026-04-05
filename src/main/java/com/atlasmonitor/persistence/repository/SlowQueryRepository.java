package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.QueryShapeNormalizer;
import com.atlasmonitor.application.model.QueryShapeStats;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.persistence.dao.SlowQueryDao;
import com.atlasmonitor.persistence.document.SlowQueryDocument;
import com.mongodb.MongoBulkWriteException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SlowQueryRepository {

    private final SlowQueryDao dao;
    private final MongoTemplate mongoTemplate;
    private final ConversionService conversionService;
    private final QueryShapeNormalizer queryShapeNormalizer;

    public Optional<SlowQuery> findSampleByShapeHash(String shapeHash) {
        return dao.findTopByShapeHashOrderByOccurredAtDesc(shapeHash)
            .map(doc -> conversionService.convert(doc, SlowQuery.class));
    }

    public List<SlowQuery> findByDateRange(Instant from, Instant endDate, Long minDurationMillis) {
        Instant until = endDate != null ? endDate : Instant.now();
        long minDuration = minDurationMillis != null ? minDurationMillis : 0;

        return dao.findByOccurredAtBetweenAndDurationMillisGreaterThanEqualOrderByOccurredAtDesc(from, until, minDuration)
            .stream()
            .map(doc -> conversionService.convert(doc, SlowQuery.class))
            .toList();
    }

    public List<QueryShapeStats> aggregateByShape(Instant from, Instant until) {
        var aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("occurredAt").gte(from).lte(until)
                .and("shapeHash").ne(null)),
            Aggregation.group("shapeHash")
                .first("namespace").as("namespace")
                .first("planSummary").as("planSummary")
                .first("normalizedFilter").as("normalizedFilter")
                .count().as("queryCount")
                .sum("durationMillis").as("totalDurationMillis")
                .avg("durationMillis").as("avgDurationMillis")
                .max("durationMillis").as("maxDurationMillis")
                .min("durationMillis").as("minDurationMillis"),
            Aggregation.sort(Sort.Direction.DESC, "totalDurationMillis")
        );

        return mongoTemplate.aggregate(aggregation, "slow_queries", ShapeStatsResult.class)
            .getMappedResults()
            .stream()
            .map(r -> new QueryShapeStats(
                r.id, r.namespace, r.planSummary, r.normalizedFilter,
                r.queryCount, r.totalDurationMillis, r.avgDurationMillis,
                r.maxDurationMillis, r.minDurationMillis))
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

                var filter = it.shape().filter();
                var normalizedFilter = queryShapeNormalizer.extractShape(filter);
                document.setNormalizedFilter(normalizedFilter);
                document.setShapeHash(queryShapeNormalizer.computeShapeHash(it.namespace(), normalizedFilter));

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

    private static class ShapeStatsResult {
        public String id;
        public String namespace;
        public String planSummary;
        public String normalizedFilter;
        public long queryCount;
        public long totalDurationMillis;
        public double avgDurationMillis;
        public long maxDurationMillis;
        public long minDurationMillis;
    }
}
