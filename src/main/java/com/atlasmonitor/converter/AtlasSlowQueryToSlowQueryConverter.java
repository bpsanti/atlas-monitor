package com.atlasmonitor.converter;

import com.atlasmonitor.client.resource.AtlasSlowQueryMetricsResource;
import com.atlasmonitor.client.resource.AtlasSlowQueryResource;
import com.atlasmonitor.application.model.SlowQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AtlasSlowQueryToSlowQueryConverter implements Converter<AtlasSlowQueryResource, SlowQuery> {

    private final ObjectMapper objectMapper;

    private static final AtlasSlowQueryMetricsResource EMPTY_METRICS =
        new AtlasSlowQueryMetricsResource(null, null, null, null, null, null, null, null, null, null);

    @Override
    public SlowQuery convert(AtlasSlowQueryResource source) {
        var metrics = source.metrics() != null ? source.metrics() : EMPTY_METRICS;

        try {
            JsonNode root = objectMapper.readTree(source.line());
            JsonNode attr = root.path("attr");

            long durationMillis = metrics.operationExecutionTime() != null
                ? metrics.operationExecutionTime()
                : attr.path("durationMillis").asLong(0);

            return new SlowQuery(
                Instant.parse(root.path("t").path("$date").asText()),
                attr.path("type").asText(null),
                attr.path("ns").asText(source.namespace()),
                durationMillis,
                textOrNull(attr, "planSummary"),
                metrics.keysExamined(),
                metrics.docsExamined(),
                metrics.docsReturned(),
                metrics.docsExaminedReturnedRatio(),
                metrics.keysExaminedReturnedRatio(),
                metrics.hasIndexCoverage(),
                metrics.hasSort(),
                metrics.operationExecutionTime(),
                metrics.responseLength(),
                metrics.numYields(),
                textOrNull(attr, "remote"),
                booleanOrNull(attr, "cursorExhausted"),
                parseFilter(attr)
            );
        } catch (Exception e) {
            return new SlowQuery(
                null, null, source.namespace(), 0, null,
                metrics.keysExamined(), metrics.docsExamined(), metrics.docsReturned(),
                metrics.docsExaminedReturnedRatio(), metrics.keysExaminedReturnedRatio(),
                metrics.hasIndexCoverage(), metrics.hasSort(), metrics.operationExecutionTime(),
                metrics.responseLength(), metrics.numYields(),
                null, null, null
            );
        }
    }

    private String parseFilter(JsonNode attr) {
        JsonNode commandNode = attr.has("originatingCommand")
            ? attr.path("originatingCommand")
            : attr.path("command");
        JsonNode filterNode = commandNode.path("filter");
        if (filterNode.isMissingNode()) {
            filterNode = commandNode.path("pipeline");
        }
        return filterNode.isMissingNode() ? null : filterNode.toPrettyString();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }

    private Boolean booleanOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        return child.isMissingNode() || child.isNull() ? null : child.asBoolean();
    }
}
