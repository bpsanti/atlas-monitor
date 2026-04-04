package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.model.SlowQueryAnalysis;
import com.atlasmonitor.persistence.dao.SlowQueryAnalysisDao;
import com.atlasmonitor.persistence.document.SlowQueryAnalysisDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SlowQueryAnalysisRepository {

    private final SlowQueryAnalysisDao dao;

    public Optional<SlowQueryAnalysis> findByShapeHash(String shapeHash) {
        return dao.findByShapeHash(shapeHash)
            .map(doc -> new SlowQueryAnalysis(
                doc.getShapeHash(), doc.getNamespace(), doc.getPlanSummary(),
                doc.getAnalysis(), doc.getAnalyzedAt(), true));
    }

    public void save(
        String shapeHash,
        String namespace,
        String planSummary,
        String normalizedFilter,
        String analysis
    ) {
        var doc = new SlowQueryAnalysisDocument();
        doc.setShapeHash(shapeHash);
        doc.setNamespace(namespace);
        doc.setPlanSummary(planSummary);
        doc.setNormalizedFilter(normalizedFilter);
        doc.setAnalysis(analysis);
        doc.setAnalyzedAt(Instant.now());
        dao.save(doc);
    }
}
