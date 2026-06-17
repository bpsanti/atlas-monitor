package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.model.CodeAnalysis;
import com.atlasmonitor.application.model.SlowQueryAnalysis;
import com.atlasmonitor.persistence.dao.SlowQueryAnalysisDao;
import com.atlasmonitor.persistence.document.SlowQueryAnalysisDocument;
import com.atlasmonitor.persistence.document.SlowQueryAnalysisDocument.CodeAnalysisEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SlowQueryAnalysisRepository {

    private final SlowQueryAnalysisDao dao;

    public Optional<SlowQueryAnalysis> findByShapeHash(String shapeHash) {
        return dao.findByShapeHash(shapeHash)
            .map(doc -> new SlowQueryAnalysis(
                doc.getShapeHash(),
                doc.getNamespace(),
                doc.getPlanSummary(),
                doc.getAnalysis(),
                doc.getDatabaseAnalysis(),
                toCodeAnalyses(doc.getCodeAnalyses()),
                doc.getAnalyzedAt(),
                true
            ));
    }

    public void deleteByShapeHash(String shapeHash) {
        dao.deleteByShapeHash(shapeHash);
    }

    public void save(
        String shapeHash,
        String namespace,
        String planSummary,
        String normalizedFilter,
        String analysis,
        String databaseAnalysis,
        List<CodeAnalysis> codeAnalyses
    ) {
        var doc = new SlowQueryAnalysisDocument();
        doc.setShapeHash(shapeHash);
        doc.setNamespace(namespace);
        doc.setPlanSummary(planSummary);
        doc.setNormalizedFilter(normalizedFilter);
        doc.setAnalysis(analysis);
        doc.setDatabaseAnalysis(databaseAnalysis);
        doc.setCodeAnalyses(toCodeAnalysisEntries(codeAnalyses));
        doc.setAnalyzedAt(Instant.now());
        dao.save(doc);
    }

    private List<CodeAnalysis> toCodeAnalyses(List<CodeAnalysisEntry> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
            .map(e -> new CodeAnalysis(
                e.getFilePath(),
                e.getRepositoryName(),
                e.getHtmlUrl(),
                e.getLineNumber(),
                e.getAnalysis()
            ))
            .toList();
    }

    private List<CodeAnalysisEntry> toCodeAnalysisEntries(List<CodeAnalysis> analyses) {
        if (analyses == null) {
            return List.of();
        }
        return analyses.stream()
            .map(a -> new CodeAnalysisEntry(
                a.filePath(),
                a.repositoryName(),
                a.htmlUrl(),
                a.lineNumber(),
                a.analysis()
            ))
            .toList();
    }
}
