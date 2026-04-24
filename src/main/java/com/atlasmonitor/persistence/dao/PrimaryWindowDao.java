package com.atlasmonitor.persistence.dao;

import com.atlasmonitor.persistence.document.PrimaryWindowDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PrimaryWindowDao extends MongoRepository<PrimaryWindowDocument, String> {

    List<PrimaryWindowDocument> findByFromLessThanEqualAndUntilGreaterThanEqualOrderByFromAsc(
        Instant until, Instant from);

    Optional<PrimaryWindowDocument> findTopByOrderByUntilDesc();
}
