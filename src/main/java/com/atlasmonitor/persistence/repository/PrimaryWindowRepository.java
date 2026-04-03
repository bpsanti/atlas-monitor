package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.persistence.document.PrimaryWindowDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface PrimaryWindowRepository extends MongoRepository<PrimaryWindowDocument, String> {

    List<PrimaryWindowDocument> findByFromLessThanEqualAndUntilGreaterThanEqualOrderByFromAsc(
        Instant until, Instant from);
}
