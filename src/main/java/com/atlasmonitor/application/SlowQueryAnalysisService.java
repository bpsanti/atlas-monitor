package com.atlasmonitor.application;

import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.application.model.SlowQueryAnalysis;
import com.atlasmonitor.client.ClaudeClient;
import com.atlasmonitor.persistence.repository.SlowQueryAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlowQueryAnalysisService {

    private final ClaudeClient claudeClient;
    private final SlowQueryAnalysisRepository analysisRepository;
    private final QueryShapeNormalizer queryShapeNormalizer;

    public Optional<SlowQueryAnalysis> findAnalysis(SlowQuery query) {
        var analysisHash = computeAnalysisHash(query);
        return analysisRepository.findByShapeHash(analysisHash);
    }

    public SlowQueryAnalysis analyze(SlowQuery query) {
        var shapeHash = computeAnalysisHash(query);
        var cached = analysisRepository.findByShapeHash(shapeHash);

        if (cached.isPresent()) {
            log.info("Analysis cache hit for shape hash: {}", shapeHash);
            return cached.get();
        }

        log.info("Analysis cache miss for shape hash: {}, calling Claude", shapeHash);
        var analysisText = claudeClient.analyzeSlowQuery(query);
        var normalizedFilter = queryShapeNormalizer.extractShape(query.shape().filter());

        analysisRepository.save(
            shapeHash,
            query.namespace(),
            query.shape().planSummary(),
            normalizedFilter,
            analysisText
        );

        return new SlowQueryAnalysis(
            shapeHash,
            query.namespace(),
            query.shape().planSummary(),
            analysisText,
            Instant.now(),
            false
        );
    }

    private String computeAnalysisHash(SlowQuery query) {
        var normalizedFilter = queryShapeNormalizer.extractShape(query.shape().filter());
        return queryShapeNormalizer.computeAnalysisHash(query.namespace(), query.shape().planSummary(), normalizedFilter);
    }
}
