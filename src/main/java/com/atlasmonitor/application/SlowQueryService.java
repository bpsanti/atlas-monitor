package com.atlasmonitor.application;

import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasSlowQueryResource;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.config.SyncProperties;
import com.atlasmonitor.persistence.document.SlowQueryDocument;
import com.atlasmonitor.persistence.repository.SlowQueryRepository;
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
    private final SlowQueryRepository slowQueryRepository;
    private final ConversionService conversionService;
    private final SyncProperties syncProperties;

    public List<SlowQuery> getSlowQueries(
        Instant since,
        Long durationMs,
        Long minDurationMillis,
        Integer nLogs
    ) {
        long retentionMinDurationMs = syncProperties.slowQueryMinDuration().toMillis();
        boolean belowRetentionThreshold = minDurationMillis != null && minDurationMillis < retentionMinDurationMs;

        if (belowRetentionThreshold) {
            return getSlowQueries(resolvePrimaryProcessId(), since, durationMs, minDurationMillis, nLogs);
        }
        return getStoredSlowQueries(since, durationMs, minDurationMillis);
    }

    public List<SlowQuery> getSlowQueries(
        String processId,
        Instant since,
        Long durationMs,
        Long minDurationMillis,
        Integer nLogs
    ) {
        Long sinceEpoch = since != null ? since.toEpochMilli() : null;

        var response = atlasApiClient.getSlowQueryLogs(processId, sinceEpoch, durationMs, nLogs);

        List<AtlasSlowQueryResource> queries = response.slowQueries();
        if (queries == null) {
            return List.of();
        }

        return queries.stream()
            .map(q -> conversionService.convert(q, SlowQuery.class))
            .filter(q -> minDurationMillis == null || q.durationMillis() >= minDurationMillis)
            .toList();
    }

    private List<SlowQuery> getStoredSlowQueries(Instant since, Long durationMs, Long minDurationMillis) {
        Instant until = durationMs != null ? since.plusMillis(durationMs) : Instant.now();
        long minDuration = minDurationMillis != null ? minDurationMillis : 0;

        List<SlowQueryDocument> docs = slowQueryRepository
            .findByDateBetweenAndDurationMillisGreaterThanEqualOrderByDateDesc(since, until, minDuration);

        return docs.stream()
            .map(doc -> conversionService.convert(doc, SlowQuery.class))
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
