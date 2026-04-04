package com.atlasmonitor.application;

import com.atlasmonitor.application.model.IopsMetrics;
import com.atlasmonitor.application.model.PrimaryWindow;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.config.SyncProperties;
import com.atlasmonitor.persistence.repository.IopsMetricsRepository;
import com.atlasmonitor.persistence.repository.PrimaryWindowRepository;
import com.atlasmonitor.persistence.repository.SlowQueryRepository;
import com.atlasmonitor.persistence.repository.SyncMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncService {

    private static final String SLOW_QUERY_SYNC_ID = "slow_query_sync";
    private static final String PRIMARY_WINDOW_SYNC_ID = "primary_window_sync";
    private static final String IOPS_SYNC_ID = "iops_sync";

    private final SlowQueryService slowQueryService;
    private final IopsService iopsService;
    private final PrimaryReplicaResolutionService primaryResolutionService;
    private final SlowQueryRepository slowQueryRepository;
    private final PrimaryWindowRepository primaryWindowRepository;
    private final IopsMetricsRepository iopsMetricsRepository;
    private final SyncMetadataRepository syncMetadataRepository;
    private final SyncProperties syncProperties;

    @Scheduled(fixedDelayString = "${atlas.sync.interval}")
    public void sync() {
        syncPrimaryWindows();
        syncSlowQueries();
        syncIops();
    }

    private void syncPrimaryWindows() {
        log.info("Starting primary window sync");

        Instant now = Instant.now();
        Instant from = syncMetadataRepository.getLastSyncedUntil(PRIMARY_WINDOW_SYNC_ID)
            .orElse(now.minus(syncProperties.primaryWindowInitialLookback()));

        List<PrimaryWindow> windows = primaryResolutionService.resolvePrimaryWindows("PT1H", from, now);
        if (windows.isEmpty()) {
            log.info("Primary window sync skipped: no windows found");
            return;
        }

        for (PrimaryWindow window : windows) {
            primaryWindowRepository.save(window);
        }

        Instant lastUntil = windows.stream()
            .map(PrimaryWindow::until)
            .max(Instant::compareTo)
            .orElse(from);

        syncMetadataRepository.updateLastSynced(PRIMARY_WINDOW_SYNC_ID, lastUntil);
        log.info("Primary window sync complete: {} windows processed, synced until {}", windows.size(), lastUntil);
    }

    private void syncSlowQueries() {
        log.info("Starting slow query sync");

        Instant now = Instant.now();
        Instant from = syncMetadataRepository.getLastSyncedUntil(SLOW_QUERY_SYNC_ID)
            .orElse(now.minus(syncProperties.slowQueryInitialLookback()));

        if (!from.isBefore(now)) {
            log.info("Slow query sync skipped: already up to date");
            return;
        }

        long minDurationMs = syncProperties.slowQueryMinDuration().toMillis();
        Duration batchWindow = syncProperties.slowQueryBatchWindow();

        List<PrimaryWindow> primaryWindows = primaryWindowRepository.findOverlapping(from, now);
        log.info("Found {} primary windows for range [{} - {}]", primaryWindows.size(), from, now);

        int inserted = 0;
        int total = 0;
        Instant lastSyncedUntil = from;

        for (int index = 0; index < primaryWindows.size(); index++) {
            PrimaryWindow window = primaryWindows.get(index);
            Instant windowFrom = window.from().isBefore(from) ? from : window.from();
            Instant windowUntil = window.until().isAfter(now) ? now : window.until();

            log.info("Processing primary window {}/{}: {} [{} - {}]",
                index + 1, primaryWindows.size(), window.processId(), windowFrom, windowUntil);

            Instant batchStart = windowFrom;
            int batchNum = 0;

            while (batchStart.isBefore(windowUntil)) {
                Instant batchEnd = batchStart.plus(batchWindow);
                if (batchEnd.isAfter(windowUntil)) {
                    batchEnd = windowUntil;
                }
                batchNum++;

                List<SlowQuery> queries = slowQueryService.getSlowQueriesFromAtlas(
                    window.processId(),
                    batchStart,
                    batchEnd,
                    minDurationMs
                );

                int batchInserted = slowQueryRepository.insertAll(queries, window.processId());

                total += queries.size();
                inserted += batchInserted;
                if (batchEnd.isAfter(lastSyncedUntil)) {
                    lastSyncedUntil = batchEnd;
                }
                log.info("  Batch {} [{} - {}]: fetched {}, inserted {} (duplicates skipped: {})",
                    batchNum, batchStart, batchEnd, queries.size(), batchInserted, queries.size() - batchInserted);

                batchStart = batchEnd;
            }
        }

        syncMetadataRepository.updateLastSynced(SLOW_QUERY_SYNC_ID, lastSyncedUntil);
        log.info("Slow query sync complete: {} new queries stored (total fetched: {}), synced until {}",
            inserted, total, lastSyncedUntil);
    }

    private void syncIops() {
        log.info("Starting IOPS sync");

        Instant now = Instant.now();
        Instant from = syncMetadataRepository.getLastSyncedUntil(IOPS_SYNC_ID)
            .orElse(now.minus(syncProperties.iopsInitialLookback()));

        if (!from.isBefore(now)) {
            log.info("IOPS sync skipped: already up to date");
            return;
        }

        List<PrimaryWindow> primaryWindows = primaryWindowRepository.findOverlapping(from, now);
        log.info("Found {} primary windows for IOPS range [{} - {}]", primaryWindows.size(), from, now);

        int inserted = 0;
        Instant lastSyncedUntil = from;

        for (int index = 0; index < primaryWindows.size(); index++) {
            PrimaryWindow window = primaryWindows.get(index);
            Instant windowFrom = window.from().isBefore(from) ? from : window.from();
            Instant windowUntil = window.until().isAfter(now) ? now : window.until();

            log.info("Syncing IOPS for primary window {}/{}: {} [{} - {}]",
                index + 1, primaryWindows.size(), window.processId(), windowFrom, windowUntil);

            IopsMetrics metrics = iopsService.queryIopsForNode(
                window.processId(),
                window.hostname(),
                "REPLICA_PRIMARY",
                "PT1H",
                windowFrom,
                windowUntil
            );

            if (!hasDataPoints(metrics)) {
                log.info("  Skipping: no data points for window [{} - {}]", windowFrom, windowUntil);
                continue;
            }

            inserted += iopsMetricsRepository.insertAll(List.of(metrics));
            if (metrics.end().isAfter(lastSyncedUntil)) {
                lastSyncedUntil = metrics.end();
            }
        }

        if (inserted > 0) {
            syncMetadataRepository.updateLastSynced(IOPS_SYNC_ID, lastSyncedUntil);
        }
        log.info("IOPS sync complete: {} entries stored, synced until {}", inserted, lastSyncedUntil);
    }

    private boolean hasDataPoints(IopsMetrics metrics) {
        return metrics.read() != null && !metrics.read().dataPoints().isEmpty();
    }
}
