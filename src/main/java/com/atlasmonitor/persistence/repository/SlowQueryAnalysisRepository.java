package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.persistence.document.SlowQueryAnalysisDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SlowQueryAnalysisRepository extends MongoRepository<SlowQueryAnalysisDocument, String> {

    Optional<SlowQueryAnalysisDocument> findByShapeHash(String shapeHash);
}
