package com.atlasmonitor.application;

import com.atlasmonitor.application.model.PrimaryWindow;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.config.SyncProperties;
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

    private final SlowQueryService slowQueryService;
    private final PrimaryReplicaResolutionService primaryResolutionService;
    private final SlowQueryRepository slowQueryRepository;
    private final PrimaryWindowRepository primaryWindowRepository;
    private final SyncMetadataRepository syncMetadataRepository;
    private final SyncProperties syncProperties;

    @Scheduled(fixedDelayString = "${atlas.sync.interval}")
    public void syncPrimaryWindows() {
        log.info("Starting primary window sync");

        Instant now = Instant.now();
        Instant from = syncMetadataRepository.computeSyncFrom(
            PRIMARY_WINDOW_SYNC_ID, syncProperties.primaryWindowInitialLookback(), syncProperties.syncWindowOverlap());

        List<PrimaryWindow> windows = primaryResolutionService.resolvePrimaryWindows("PT1H", from, now);

        for (PrimaryWindow window : windows) {
            primaryWindowRepository.save(window);
        }

        syncMetadataRepository.updateLastSynced(PRIMARY_WINDOW_SYNC_ID, now);
        log.info("Primary window sync complete: {} windows processed", windows.size());
    }

    @Scheduled(fixedDelayString = "${atlas.sync.interval}")
    public void syncSlowQueries() {
        log.info("Starting slow query sync");

        Instant now = Instant.now();
        Instant from = syncMetadataRepository.computeSyncFrom(
            SLOW_QUERY_SYNC_ID, syncProperties.slowQueryInitialLookback(), syncProperties.syncWindowOverlap());
        long minDurationMs = syncProperties.slowQueryMinDuration().toMillis();
        Duration batchWindow = syncProperties.slowQueryBatchWindow();

        List<PrimaryWindow> primaryWindows = primaryResolutionService.resolvePrimaryWindows("PT1H", from, now);
        log.info("Resolved {} primary windows for range [{} - {}]", primaryWindows.size(), from, now);

        int inserted = 0;
        int total = 0;

        for (int wi = 0; wi < primaryWindows.size(); wi++) {
            PrimaryWindow window = primaryWindows.get(wi);
            log.info("Processing primary window {}/{}: {} [{} - {}]",
                wi + 1, primaryWindows.size(), window.processId(), window.from(), window.until());

            Instant batchStart = window.from();
            int batchNum = 0;

            while (batchStart.isBefore(window.until())) {
                Instant batchEnd = batchStart.plus(batchWindow);
                if (batchEnd.isAfter(window.until())) {
                    batchEnd = window.until();
                }
                batchNum++;

                long batchDurationMs = Duration.between(batchStart, batchEnd).toMillis();

                List<SlowQuery> queries = slowQueryService.getSlowQueries(
                    window.processId(), batchStart, batchDurationMs, minDurationMs, null);

                int batchInserted = 0;
                for (SlowQuery query : queries) {
                    if (slowQueryRepository.insertIfAbsent(query, window.processId())) {
                        batchInserted++;
                    }
                }

                total += queries.size();
                inserted += batchInserted;
                log.info("  Batch {} [{} - {}]: fetched {}, inserted {} (duplicates skipped: {})",
                    batchNum, batchStart, batchEnd, queries.size(), batchInserted, queries.size() - batchInserted);

                batchStart = batchEnd;
            }
        }

        syncMetadataRepository.updateLastSynced(SLOW_QUERY_SYNC_ID, now);
        log.info("Slow query sync complete: {} new queries stored (total fetched: {})", inserted, total);
    }
}
