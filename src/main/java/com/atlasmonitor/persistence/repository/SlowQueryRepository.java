package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.persistence.document.SlowQueryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface SlowQueryRepository extends MongoRepository<SlowQueryDocument, String> {

    List<SlowQueryDocument> findByDateBetweenAndDurationMillisGreaterThanEqualOrderByDateDesc(
        Instant from, Instant until, long minDurationMillis);

    List<SlowQueryDocument> findByDateBetweenOrderByDateDesc(Instant from, Instant until);
}
