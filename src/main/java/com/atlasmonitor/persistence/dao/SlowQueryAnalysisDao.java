package com.atlasmonitor.persistence.dao;

import com.atlasmonitor.persistence.document.SlowQueryAnalysisDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SlowQueryAnalysisDao extends MongoRepository<SlowQueryAnalysisDocument, String> {

    Optional<SlowQueryAnalysisDocument> findByShapeHash(String shapeHash);

    void deleteByShapeHash(String shapeHash);
}
