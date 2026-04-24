package com.atlasmonitor.persistence.dao;

import com.atlasmonitor.persistence.document.SlowQueryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SlowQueryDao extends MongoRepository<SlowQueryDocument, String> {

    List<SlowQueryDocument> findByOccurredAtBetweenAndDurationMillisGreaterThanEqualOrderByOccurredAtDesc(
        Instant from, Instant until, long minDurationMillis);

    List<SlowQueryDocument> findByOccurredAtBetweenOrderByOccurredAtDesc(Instant from, Instant until);

    Optional<SlowQueryDocument> findTopByShapeHashOrderByOccurredAtDesc(String shapeHash);
}
