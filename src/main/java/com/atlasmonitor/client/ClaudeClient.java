package com.atlasmonitor.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.atlasmonitor.application.model.SlowQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClaudeClient {

    private final AnthropicClient anthropicClient;

    private static final String SLOW_QUERY_SYSTEM_PROMPT = """
        You are a MongoDB performance analyst. You will receive details about a slow query \
        and must analyze its efficiency. Focus on:

        1. **Index Usage**: Based on the planSummary, determine if an appropriate index is being used. \
          Flag COLLSCAN as critical. For IXSCAN, evaluate if the index covers the query fields.
        2. **Efficiency Ratios**:
          - keysExamined vs docsReturned: ideally close to 1:1. High ratio means the index is not selective enough.
          - docsExamined vs docsReturned: ideally close to 1:1. High ratio means too many documents are being scanned.
        3. **Filter/Pipeline Analysis**: Examine the filter or aggregation pipeline for optimization opportunities \
          (e.g., missing indexes on filter fields, unselective leading fields, unnecessary stages).
        4. **Actionable Recommendations**: Suggest specific index creation commands or query rewrites.

        Be concise and practical. Use markdown formatting. \
        If the query looks efficient, say so briefly.""";

    public String analyzeSlowQuery(SlowQuery query) {
        String userMessage = buildSlowQueryMessage(query);

        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.CLAUDE_SONNET_4_6)
            .maxTokens(2048L)
            .system(SLOW_QUERY_SYSTEM_PROMPT)
            .addUserMessage(userMessage)
            .build();

        Message response = anthropicClient.messages().create(params);

        return response.content().stream()
            .flatMap(block -> block.text().stream())
            .map(textBlock -> textBlock.text())
            .findFirst()
            .orElse("No analysis available.");
    }

    private String buildSlowQueryMessage(SlowQuery query) {
        var exec = query.execution();
        var ratios = exec.ratios();

        StringBuilder sb = new StringBuilder();
        sb.append("## Slow Query Details\n\n");
        sb.append("- **Namespace**: ").append(query.namespace()).append("\n");
        sb.append("- **Type**: ").append(query.operationType()).append("\n");
        sb.append("- **Duration**: ").append(query.durationMillis()).append("ms\n");

        if (query.shape().planSummary() != null) {
            sb.append("- **Plan Summary**: ").append(query.shape().planSummary()).append("\n");
        }
        if (exec.keysExaminedCount() != null) {
            sb.append("- **Keys Examined**: ").append(exec.keysExaminedCount()).append("\n");
        }
        if (exec.docsExaminedCount() != null) {
            sb.append("- **Docs Examined**: ").append(exec.docsExaminedCount()).append("\n");
        }
        if (exec.docsReturnedCount() != null) {
            sb.append("- **Documents Returned**: ").append(exec.docsReturnedCount()).append("\n");
        }
        if (ratios.docsExaminedToReturnedRatio() != null) {
            sb.append("- **Docs Examined/Returned Ratio**: ").append(ratios.docsExaminedToReturnedRatio()).append("\n");
        }
        if (ratios.keysExaminedToReturnedRatio() != null) {
            sb.append("- **Keys Examined/Returned Ratio**: ").append(ratios.keysExaminedToReturnedRatio()).append("\n");
        }
        if (exec.hasIndexCoverage() != null) {
            sb.append("- **Has Index Coverage**: ").append(exec.hasIndexCoverage()).append("\n");
        }
        if (exec.hasSortStage() != null) {
            sb.append("- **Has Sort**: ").append(exec.hasSortStage()).append("\n");
        }
        if (exec.yieldsCount() != null) {
            sb.append("- **Yields Count**: ").append(exec.yieldsCount()).append("\n");
        }
        if (exec.isCursorExhausted() != null) {
            sb.append("- **Cursor Exhausted**: ").append(exec.isCursorExhausted()).append("\n");
        }
        if (query.shape().filter() != null) {
            sb.append("\n## Filter / Pipeline\n\n```json\n").append(query.shape().filter()).append("\n```\n");
        }

        return sb.toString();
    }
}
