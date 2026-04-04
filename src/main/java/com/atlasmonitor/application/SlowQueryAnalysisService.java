package com.atlasmonitor.application;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.application.model.SlowQueryAnalysis;
import com.atlasmonitor.persistence.document.SlowQueryAnalysisDocument;
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

    private final AnthropicClient anthropicClient;
    private final SlowQueryAnalysisRepository analysisRepository;
    private final QueryShapeNormalizer queryShapeNormalizer;

    private static final String SYSTEM_PROMPT = """
            You are a MongoDB performance analyst. You will receive details about a slow query \
            and must analyze its efficiency. Focus on:

            1. **Index Usage**: Based on the planSummary, determine if an appropriate index is being used. \
              Flag COLLSCAN as critical. For IXSCAN, evaluate if the index covers the query fields.
            2. **Efficiency Ratios**:
              - keysExamined vs nreturned: ideally close to 1:1. High ratio means the index is not selective enough.
              - docsExamined vs nreturned: ideally close to 1:1. High ratio means too many documents are being scanned.
            3. **Filter/Pipeline Analysis**: Examine the filter or aggregation pipeline for optimization opportunities \
              (e.g., missing indexes on filter fields, unselective leading fields, unnecessary stages).
            4. **Actionable Recommendations**: Suggest specific index creation commands or query rewrites.

            Be concise and practical. Use markdown formatting. \
            If the query looks efficient, say so briefly.""";

    public Optional<SlowQueryAnalysis> findAnalysis(SlowQuery query) {
        String shapeHash = queryShapeNormalizer.computeShapeHash(
            query.namespace(), query.planSummary(), query.filter());

        return analysisRepository.findByShapeHash(shapeHash)
            .map(doc -> new SlowQueryAnalysis(
                doc.getShapeHash(), doc.getNamespace(), doc.getPlanSummary(),
                doc.getAnalysis(), doc.getAnalyzedAt(), true));
    }

    public SlowQueryAnalysis analyze(SlowQuery query) {
        String shapeHash = queryShapeNormalizer.computeShapeHash(
            query.namespace(), query.planSummary(), query.filter());

        Optional<SlowQueryAnalysisDocument> cached = analysisRepository.findByShapeHash(shapeHash);
        if (cached.isPresent()) {
            log.info("Analysis cache hit for shape hash: {}", shapeHash);
            SlowQueryAnalysisDocument doc = cached.get();
            return new SlowQueryAnalysis(
                doc.getShapeHash(), doc.getNamespace(), doc.getPlanSummary(),
                doc.getAnalysis(), doc.getAnalyzedAt(), true);
        }

        log.info("Analysis cache miss for shape hash: {}, calling Claude", shapeHash);
        String analysisText = callClaude(query);
        Instant now = Instant.now();

        var doc = new SlowQueryAnalysisDocument();
        doc.setShapeHash(shapeHash);
        doc.setNamespace(query.namespace());
        doc.setPlanSummary(query.planSummary());
        doc.setNormalizedFilter(queryShapeNormalizer.extractShape(query.filter()));
        doc.setAnalysis(analysisText);
        doc.setAnalyzedAt(now);
        analysisRepository.save(doc);

        return new SlowQueryAnalysis(shapeHash, query.namespace(), query.planSummary(), analysisText, now, false);
    }

    private String callClaude(SlowQuery query) {
        String userMessage = buildUserMessage(query);

        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.CLAUDE_SONNET_4_6)
            .maxTokens(2048L)
            .system(SYSTEM_PROMPT)
            .addUserMessage(userMessage)
            .build();

        Message response = anthropicClient.messages().create(params);

        return response.content().stream()
            .flatMap(block -> block.text().stream())
            .map(textBlock -> textBlock.text())
            .findFirst()
            .orElse("No analysis available.");
    }

    private String buildUserMessage(SlowQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Slow Query Details\n\n");
        sb.append("- **Namespace**: ").append(query.namespace()).append("\n");
        sb.append("- **Type**: ").append(query.type()).append("\n");
        sb.append("- **Duration**: ").append(query.durationMillis()).append("ms\n");

        if (query.planSummary() != null) {
            sb.append("- **Plan Summary**: ").append(query.planSummary()).append("\n");
        }
        if (query.keysExamined() != null) {
            sb.append("- **Keys Examined**: ").append(query.keysExamined()).append("\n");
        }
        if (query.docsExamined() != null) {
            sb.append("- **Docs Examined**: ").append(query.docsExamined()).append("\n");
        }
        if (query.nreturned() != null) {
            sb.append("- **Documents Returned**: ").append(query.nreturned()).append("\n");
        }
        if (query.docsExaminedReturnedRatio() != null) {
            sb.append("- **Docs Examined/Returned Ratio**: ").append(query.docsExaminedReturnedRatio()).append("\n");
        }
        if (query.keysExaminedReturnedRatio() != null) {
            sb.append("- **Keys Examined/Returned Ratio**: ").append(query.keysExaminedReturnedRatio()).append("\n");
        }
        if (query.hasIndexCoverage() != null) {
            sb.append("- **Has Index Coverage**: ").append(query.hasIndexCoverage()).append("\n");
        }
        if (query.hasSort() != null) {
            sb.append("- **Has Sort**: ").append(query.hasSort()).append("\n");
        }
        if (query.numYields() != null) {
            sb.append("- **Num Yields**: ").append(query.numYields()).append("\n");
        }
        if (query.cursorExhausted() != null) {
            sb.append("- **Cursor Exhausted**: ").append(query.cursorExhausted()).append("\n");
        }
        if (query.filter() != null) {
            sb.append("\n## Filter / Pipeline\n\n```json\n").append(query.filter()).append("\n```\n");
        }

        return sb.toString();
    }
}
