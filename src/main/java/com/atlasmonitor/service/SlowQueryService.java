package com.atlasmonitor.service;

import com.atlasmonitor.api.dto.SlowQueryResponse;
import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasSlowQueryResource;
import com.atlasmonitor.client.resource.AtlasSlowQueryWrapperResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SlowQueryService {

    private final AtlasApiClient atlasApiClient;
    private final ObjectMapper objectMapper;

    public List<SlowQueryResponse> getSlowQueries(
            Instant since,
            Long durationMs,
            Long minDurationMillis,
            Integer nLogs
    ) {
        String primaryProcessId = resolvePrimaryProcessId();
        Long sinceEpoch = since != null ? since.toEpochMilli() : null;

        AtlasSlowQueryWrapperResource response = atlasApiClient.getSlowQueryLogs(
                primaryProcessId, sinceEpoch, durationMs, nLogs);

        List<AtlasSlowQueryResource> queries = response.slowQueries();
        if (queries == null) {
            return List.of();
        }

         return queries.stream()
                .map(this::parseLine)
                .filter(q -> minDurationMillis == null || q.durationMillis() >= minDurationMillis)
                .toList();
    }

    private SlowQueryResponse parseLine(AtlasSlowQueryResource slowQuery) {
        try {
            JsonNode root = objectMapper.readTree(slowQuery.line());
            JsonNode attr = root.path("attr");

            Instant date = Instant.parse(root.path("t").path("$date").asText());
            String type = attr.path("type").asText(null);
            String namespace = attr.path("ns").asText(slowQuery.namespace());
            long durationMillis = attr.path("durationMillis").asLong(0);
            String planSummary = textOrNull(attr, "planSummary");
            Long keysExamined = longOrNull(attr, "keysExamined");
            Long docsExamined = longOrNull(attr, "docsExamined");
            Long nreturned = longOrNull(attr, "nreturned");
            String remote = textOrNull(attr, "remote");
            Boolean cursorExhausted = booleanOrNull(attr, "cursorExhausted");

            JsonNode commandNode = attr.has("originatingCommand") ? attr.path("originatingCommand") : attr.path("command");
            JsonNode filterNode = commandNode.path("filter");
            if (filterNode.isMissingNode()) {
                filterNode = commandNode.path("pipeline");
            }
            String filter = filterNode.isMissingNode() ? null : filterNode.toPrettyString();

            return new SlowQueryResponse(date, type, namespace, durationMillis,
                    planSummary, keysExamined, docsExamined, nreturned,
                    remote, cursorExhausted, filter);
        } catch (Exception e) {
            return new SlowQueryResponse(null, null, slowQuery.namespace(), 0,
                    null, null, null, null, null, null, null);
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asLong();
    }

    private Boolean booleanOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asBoolean();
    }

    private String resolvePrimaryProcessId() {
        return atlasApiClient.listReplicas().results().stream()
                .filter(r -> r.typeName() != null && r.typeName().contains("PRIMARY"))
                .map(r -> r.id())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No primary replica found"));
    }
}
