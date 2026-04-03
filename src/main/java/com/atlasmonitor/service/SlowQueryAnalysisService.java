package com.atlasmonitor.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.atlasmonitor.api.dto.SlowQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SlowQueryAnalysisService {

    private final AnthropicClient anthropicClient;

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

    public String analyze(SlowQueryResponse query) {
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

    private String buildUserMessage(SlowQueryResponse query) {
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
        if (query.cursorExhausted() != null) {
            sb.append("- **Cursor Exhausted**: ").append(query.cursorExhausted()).append("\n");
        }
        if (query.filter() != null) {
            sb.append("\n## Filter / Pipeline\n\n```json\n").append(query.filter()).append("\n```\n");
        }

        return sb.toString();
    }
}
