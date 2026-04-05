package com.atlasmonitor.application;

import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasSlowQueryResource;
import com.atlasmonitor.application.model.QueryShapeStats;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.config.SyncProperties;
import com.atlasmonitor.persistence.repository.SlowQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SlowQueryService {

    private final AtlasApiClient atlasApiClient;
    private final SlowQueryRepository slowQueryRepository;
    private final ConversionService conversionService;
    private final SyncProperties syncProperties;

    public List<SlowQuery> getSlowQueries(
        Instant startDate,
        Instant endDate,
        Long minDurationMillis
    ) {
        long retentionMinDurationMs = syncProperties.slowQueryMinDuration().toMillis();
        boolean belowRetentionThreshold = minDurationMillis != null && minDurationMillis < retentionMinDurationMs;

        if (belowRetentionThreshold) {
            return getSlowQueriesFromAtlas(resolvePrimaryProcessId(), startDate, endDate, minDurationMillis);
        }

        return slowQueryRepository.findByDateRange(startDate, endDate, minDurationMillis);
    }

    public List<QueryShapeStats> getQueryShapeStats(Instant startDate, Instant endDate) {
        Instant until = endDate != null ? endDate : Instant.now();
        return slowQueryRepository.aggregateByShape(startDate, until);
    }

    public Optional<SlowQuery> findSampleByShapeHash(String shapeHash) {
        return slowQueryRepository.findSampleByShapeHash(shapeHash);
    }

    public List<SlowQuery> getSlowQueriesFromAtlas(
        String processId,
        Instant startDate,
        Instant endDate,
        Long minDurationMillis
    ) {
        var response = atlasApiClient.getSlowQueryLogs(processId, startDate, endDate);

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
