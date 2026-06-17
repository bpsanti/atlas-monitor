package com.atlasmonitor.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.atlasmonitor.application.model.CodeAnalysis;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.config.GitHubProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class ClaudeClient {

    private static final int MAX_TOOL_ITERATIONS = 15;
    private static final int MAX_FILE_CONTENT_CHARS = 15000;

    private final AnthropicClient anthropicClient;
    private final GitHubCodeSearchClient gitHubCodeSearchClient;
    private final GitHubProperties gitHubProperties;
    private final ObjectMapper objectMapper;

    private final Tool readFileTool;

    public ClaudeClient(AnthropicClient anthropicClient,
                        GitHubCodeSearchClient gitHubCodeSearchClient,
                        GitHubProperties gitHubProperties,
                        ObjectMapper objectMapper) {
        this.anthropicClient = anthropicClient;
        this.gitHubCodeSearchClient = gitHubCodeSearchClient;
        this.gitHubProperties = gitHubProperties;
        this.objectMapper = objectMapper;
        this.readFileTool = buildReadFileTool();
    }

    private static final String SLOW_QUERY_SYSTEM_PROMPT = """
        You are a MongoDB performance analyst with access to the application's source code via GitHub.

        You have one tool:
        - **read_file**: Read the full content of a specific file from GitHub.

        You will receive the full list of Java files in the repository. Use this to identify \
        which files to read based on their names and paths.

        ## Workflow

        1. Analyze the slow query details (namespace, filter fields, plan, metrics).
        2. Look at the file list to identify relevant files. For a query on collection T_BILL \
           with fields {farmId, is_deleted, date}, look for files like:
           - *Predicates*.java (e.g., BillPredicates.java) — defines the predicate methods
           - *Service*.java or *ServiceImpl*.java (e.g., BillServiceImpl.java) — builds the query
           - *Repository*.java (e.g., BillRepository.java) — repository interface/implementation
        3. Use **read_file** to read the most promising files.
        4. Follow the code trail: if a Service uses Predicates, read both files.
        5. Find the specific method that builds a query using ALL the filter fields.
        6. Produce your final analysis.

        **IMPORTANT**: You have a maximum of 15 tool call iterations. Each read_file counts as one. \
        Plan your reads carefully — read only the most relevant 3-5 files, then produce your final analysis. \
        Do NOT try to read every related file.

        ## Final Response Format

        Your FINAL response (after all tool use) MUST be a valid JSON object (no markdown, no code fences):
        {
          "databaseAnalysis": "markdown string with MongoDB-level analysis",
          "codeAnalyses": [
            {
              "filePath": "path/to/File.java",
              "repositoryName": "org/repo",
              "htmlUrl": "https://github.com/org/repo/blob/HEAD/path/to/File.java",
              "lineNumber": 42,
              "analysis": "markdown string with code-level finding"
            }
          ]
        }

        For **databaseAnalysis**, focus on:
        1. Index usage (flag COLLSCAN as critical, evaluate IXSCAN coverage)
        2. Efficiency ratios (keysExamined vs docsReturned, docsExamined vs docsReturned)
        3. Filter/pipeline optimization opportunities
        4. Actionable recommendations (specific index commands or query rewrites)

        For **codeAnalyses**:
        - ONLY include files where the query is actually constructed (Services with BooleanBuilder, \
          Repositories with @Query, Criteria builders, MongoTemplate calls).
        - ONLY include code that builds a query using ALL the filter fields from the slow query. \
          Partial matches are not the origin of this query — skip them.
        - NEVER include entity/document/model classes.
        - For each entry: exact file path, line number, repo name, GitHub URL, and analysis \
          explaining how the method builds the query and what can be improved.
        - If you cannot find the query origin, return an empty codeAnalyses array.

        Be concise and practical. Use markdown in the analysis strings.""";

    public AnalysisResult analyzeSlowQuery(SlowQuery query) {
        boolean hasGitHub = gitHubProperties != null
            && gitHubProperties.token() != null
            && !gitHubProperties.token().isBlank()
            && gitHubProperties.repositories() != null
            && !gitHubProperties.repositories().isEmpty();

        if (!hasGitHub) {
            return analyzeWithoutTools(query);
        }

        return analyzeWithTools(query);
    }

    private AnalysisResult analyzeWithoutTools(SlowQuery query) {
        String userMessage = buildSlowQueryMessage(query, Map.of());

        MessageCreateParams params = MessageCreateParams.builder()
            .model(Model.CLAUDE_SONNET_4_6)
            .maxTokens(3072L)
            .system(SLOW_QUERY_SYSTEM_PROMPT)
            .addUserMessage(userMessage)
            .build();

        Message response = anthropicClient.messages().create(params);
        String rawResponse = extractTextContent(response);
        return parseAnalysisResponse(rawResponse);
    }

    private AnalysisResult analyzeWithTools(SlowQuery query) {
        // Fetch file trees for all repos (one API call per repo, uses general rate limit)
        var repoTrees = new java.util.LinkedHashMap<String, List<String>>();
        for (var repo : gitHubProperties.repositories()) {
            log.info("Fetching file tree for {}", repo);
            var tree = gitHubCodeSearchClient.getRepositoryTree(repo);
            repoTrees.put(repo, tree);
            log.info("Found {} Java files in {}", tree.size(), repo);
        }

        String userMessage = buildSlowQueryMessage(query, repoTrees);

        var messages = new ArrayList<MessageParam>();
        messages.add(MessageParam.builder()
            .role(MessageParam.Role.USER)
            .content(userMessage)
            .build());

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            var params = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_4_6)
                .maxTokens(4096L)
                .system(SLOW_QUERY_SYSTEM_PROMPT)
                .messages(messages)
                .addTool(readFileTool)
                .build();

            Message response = anthropicClient.messages().create(params);

            // Add assistant response to conversation
            messages.add(MessageParam.builder()
                .role(MessageParam.Role.ASSISTANT)
                .contentOfBlockParams(response.content().stream()
                    .map(this::contentBlockToParam)
                    .toList())
                .build());

            // Check if Claude wants to use tools
            Optional<StopReason> stopReason = response.stopReason();
            if (stopReason.isEmpty() || !stopReason.get().equals(StopReason.TOOL_USE)) {
                String rawResponse = extractTextContent(response);
                log.info("Agentic analysis completed after {} iterations", iteration + 1);
                return parseAnalysisResponse(rawResponse);
            }

            // Process tool calls
            var toolResults = new ArrayList<ContentBlockParam>();
            for (ContentBlock block : response.content()) {
                if (block.toolUse().isPresent()) {
                    ToolUseBlock toolUse = block.toolUse().get();
                    log.info("Tool call: {}({}) — iteration {}", toolUse.name(),
                        summarizeInput(toolUse), iteration + 1);
                    String result = executeToolCall(toolUse);
                    toolResults.add(ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                            .toolUseId(toolUse.id())
                            .content(result)
                            .build()
                    ));
                }
            }

            // Warn Claude when running low on iterations
            int remaining = MAX_TOOL_ITERATIONS - iteration - 1;
            if (remaining <= 2) {
                toolResults.add(ContentBlockParam.ofText(
                    com.anthropic.models.messages.TextBlockParam.builder()
                        .text("⚠ You have " + remaining + " iterations left. " +
                            "Stop reading files and produce your final JSON analysis NOW with what you have.")
                        .build()
                ));
            }

            messages.add(MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(toolResults)
                .build());
        }

        log.warn("Agentic analysis exhausted {} iterations", MAX_TOOL_ITERATIONS);
        return new AnalysisResult("Analysis incomplete: max iterations reached", "", List.of());
    }

    private String summarizeInput(ToolUseBlock toolUse) {
        try {
            JsonNode input = objectMapper.valueToTree(toolUse._input());
            if (input.has("path")) {
                return input.get("path").asText();
            }
            return input.toString().substring(0, Math.min(input.toString().length(), 80));
        } catch (Exception e) {
            return "?";
        }
    }

    private ContentBlockParam contentBlockToParam(ContentBlock block) {
        if (block.text().isPresent()) {
            return ContentBlockParam.ofText(block.text().get().toParam());
        }
        if (block.toolUse().isPresent()) {
            return ContentBlockParam.ofToolUse(block.toolUse().get().toParam());
        }
        return ContentBlockParam.ofText(
            com.anthropic.models.messages.TextBlockParam.builder().text("").build()
        );
    }

    private String executeToolCall(ToolUseBlock toolUse) {
        try {
            JsonNode input = objectMapper.valueToTree(toolUse._input());

            if ("read_file".equals(toolUse.name())) {
                return executeReadFile(input);
            }

            return "Unknown tool: " + toolUse.name();
        } catch (Exception e) {
            log.warn("Tool execution failed for {}: {}", toolUse.name(), e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private String executeReadFile(JsonNode input) {
        String repository = input.has("repository") ? input.get("repository").asText() : "";
        String path = input.has("path") ? input.get("path").asText() : "";

        if (repository.isBlank() || path.isBlank()) {
            return "Error: repository and path parameters are required";
        }

        String content = gitHubCodeSearchClient.readFileContent(repository, path);

        if (content.length() > MAX_FILE_CONTENT_CHARS) {
            content = content.substring(0, MAX_FILE_CONTENT_CHARS)
                + "\n\n... (file truncated at " + MAX_FILE_CONTENT_CHARS + " chars)";
        }

        return content;
    }

    private String buildSlowQueryMessage(SlowQuery query, Map<String, List<String>> repoTrees) {
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

            var filterFields = extractFilterFieldNames(query.shape().filter());
            if (!filterFields.isEmpty()) {
                sb.append("\n## Query Filter Fields\n\n");
                sb.append("MongoDB field names: **").append(String.join(", ", filterFields)).append("**\n");
                sb.append("The code origin MUST use ALL of these fields.\n");
            }
        }

        // Append file trees
        if (!repoTrees.isEmpty()) {
            sb.append("\n## Repository File Trees\n\n");
            sb.append("Below are the Java files in each repository. Use `read_file` to read any file.\n\n");

            for (var entry : repoTrees.entrySet()) {
                sb.append("### ").append(entry.getKey()).append(" (").append(entry.getValue().size()).append(" Java files)\n\n");
                // Filter to show only potentially relevant files to save context
                var relevantFiles = filterRelevantFiles(entry.getValue(), query.namespace());
                for (var file : relevantFiles) {
                    sb.append("- ").append(file).append("\n");
                }
                if (relevantFiles.size() < entry.getValue().size()) {
                    sb.append("- ... and ").append(entry.getValue().size() - relevantFiles.size())
                        .append(" more files (ask for full list if needed)\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Filters the file tree to show only files likely relevant to the query.
     * Includes: files containing entity name, Repository, Service, Predicate, Query, Criteria, Dao, Filter, Impl.
     */
    private List<String> filterRelevantFiles(List<String> allFiles, String namespace) {
        String collectionName = namespace != null && namespace.contains(".")
            ? namespace.substring(namespace.lastIndexOf('.') + 1)
            : namespace != null ? namespace : "";

        String entityName = deriveEntityName(collectionName).toLowerCase();

        return allFiles.stream()
            .filter(path -> {
                String lower = path.toLowerCase();
                String fileName = lower.substring(lower.lastIndexOf('/') + 1);

                // Always show files containing entity name
                if (!entityName.isEmpty() && fileName.contains(entityName)) {
                    return true;
                }

                // Show common query-building patterns
                return fileName.contains("predicate")
                    || fileName.contains("repository")
                    || fileName.contains("criteria")
                    || fileName.contains("specification");
            })
            .toList();
    }

    private String deriveEntityName(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            return "";
        }
        var name = collectionName.replaceFirst("^(?i)(T_|V_|TBL_)", "");
        if (name.contains("_")) {
            return java.util.Arrays.stream(name.split("_"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(java.util.stream.Collectors.joining());
        }
        var result = name.substring(0, 1).toUpperCase() + name.substring(1);
        if (result.length() > 3 && result.endsWith("s") && !result.endsWith("ss")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private List<String> extractFilterFieldNames(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) {
            return List.of();
        }
        try {
            var trimmed = filterJson.trim();
            if (trimmed.startsWith("[")) {
                trimmed = "{\"p\":" + trimmed + "}";
            }
            var doc = org.bson.Document.parse(trimmed);
            return extractKeys(doc).stream()
                .filter(key -> !key.startsWith("$") && !key.equals("p"))
                .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> extractKeys(org.bson.Document doc) {
        var keys = new ArrayList<String>();
        for (var entry : doc.entrySet()) {
            keys.add(entry.getKey());
            if (entry.getValue() instanceof org.bson.Document nested) {
                keys.addAll(extractKeys(nested));
            } else if (entry.getValue() instanceof List<?> list) {
                for (var item : list) {
                    if (item instanceof org.bson.Document nested) {
                        keys.addAll(extractKeys(nested));
                    }
                }
            }
        }
        return keys;
    }

    private String extractTextContent(Message response) {
        return response.content().stream()
            .flatMap(block -> block.text().stream())
            .map(textBlock -> textBlock.text())
            .findFirst()
            .orElse("{}");
    }

    private AnalysisResult parseAnalysisResponse(String rawResponse) {
        try {
            var cleaned = rawResponse.strip();
            // Strip markdown code fences in various formats
            cleaned = cleaned.replaceAll("^```(?:json)?\\s*\\n?", "").replaceAll("\\n?\\s*```$", "");
            // If still not starting with { or [, try to extract JSON from the text
            if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
                int jsonStart = cleaned.indexOf('{');
                int jsonEnd = cleaned.lastIndexOf('}');
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
                }
            }
            var parsed = objectMapper.readValue(cleaned, AnalysisJsonResponse.class);
            return new AnalysisResult(
                rawResponse,
                parsed.databaseAnalysis != null ? parsed.databaseAnalysis : "",
                parsed.codeAnalyses != null ? parsed.codeAnalyses : List.of()
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse structured analysis, falling back to raw: {}", e.getMessage());
            return new AnalysisResult(rawResponse, rawResponse, List.of());
        }
    }

    private Tool buildReadFileTool() {
        return Tool.builder()
            .name("read_file")
            .description("Read the full content of a file from a GitHub repository. " +
                "Use this to read files from the repository file tree provided in the context.")
            .inputSchema(Tool.InputSchema.builder()
                .properties(Tool.InputSchema.Properties.builder()
                    .putAdditionalProperty("repository", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "Repository full name, e.g. org/repo"
                    )))
                    .putAdditionalProperty("path", JsonValue.from(Map.of(
                        "type", "string",
                        "description", "File path within the repository"
                    )))
                    .build())
                .required(List.of("repository", "path"))
                .build())
            .build();
    }

    public record AnalysisResult(
        String analysis,
        String databaseAnalysis,
        List<CodeAnalysis> codeAnalyses
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnalysisJsonResponse(
        String databaseAnalysis,
        List<CodeAnalysis> codeAnalyses
    ) {}
}
