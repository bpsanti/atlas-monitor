package com.atlasmonitor.application;

import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasSlowQueryResource;
import com.atlasmonitor.application.model.SlowQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class SlowQueryService {

    private final AtlasApiClient atlasApiClient;
    private final ConversionService conversionService;

    public List<SlowQuery> getSlowQueries(
        Instant since,
        Long durationMs,
        Long minDurationMillis,
        Integer nLogs
    ) {
        String primaryProcessId = resolvePrimaryProcessId();
        Long sinceEpoch = since != null ? since.toEpochMilli() : null;

        var response = atlasApiClient.getSlowQueryLogs(
            primaryProcessId, sinceEpoch, durationMs, nLogs);

        List<AtlasSlowQueryResource> queries = response.slowQueries();
        if (queries == null) {
            return List.of();
        }

        return queries.stream()
            .map(q -> conversionService.convert(q, SlowQuery.class))
            .filter(q -> minDurationMillis == null || q.durationMillis() >= minDurationMillis)
            .toList();
    }

    private String resolvePrimaryProcessId() {
        return atlasApiClient.listReplicas().results().stream()
            .filter(r -> r.typeName() != null && r.typeName().contains("PRIMARY"))
            .map(r -> r.id())
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No primary replica found"));
    }
}
