package com.atlasmonitor.application;

import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.application.model.SlowQueryAnalysis;
import com.atlasmonitor.client.ClaudeClient;
import com.atlasmonitor.persistence.repository.SlowQueryAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlowQueryAnalysisService {

    private final ClaudeClient claudeClient;
    private final SlowQueryAnalysisRepository analysisRepository;
    private final QueryShapeNormalizer queryShapeNormalizer;

    public Optional<SlowQueryAnalysis> findAnalysis(SlowQuery query) {
        var shapeHash = computeShapeHash(query);
        return analysisRepository.findByShapeHash(shapeHash);
    }

    public SlowQueryAnalysis analyze(SlowQuery query) {
        var shapeHash = computeShapeHash(query);
        var cached = analysisRepository.findByShapeHash(shapeHash);

        if (cached.isPresent()) {
            log.info("Analysis cache hit for shape hash: {}", shapeHash);
            return cached.get();
        }

        log.info("Analysis cache miss for shape hash: {}, calling Claude", shapeHash);
        var analysisText = claudeClient.analyzeSlowQuery(query);

        analysisRepository.save(
            shapeHash,
            query.namespace(),
            query.shape().planSummary(),
            queryShapeNormalizer.extractShape(query.shape().filter()),
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

    private String computeShapeHash(SlowQuery query) {
        var namespace = Optional.ofNullable(query.namespace()).orElse("");
        var planSummary = Optional.ofNullable(query.shape().planSummary()).orElse("");
        var normalizedFilter = Optional.ofNullable(query.shape().filter())
            .map(queryShapeNormalizer::extractShape)
            .orElse("");

        var raw = namespace + "|" + planSummary + "|" + normalizedFilter;
        return sha256(raw);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
