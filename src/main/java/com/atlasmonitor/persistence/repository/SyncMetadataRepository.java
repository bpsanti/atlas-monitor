package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.persistence.dao.SyncMetadataDao;
import com.atlasmonitor.persistence.document.SyncMetadataDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SyncMetadataRepository {

    private final SyncMetadataDao dao;

    public Instant computeSyncFrom(String syncId, Duration initialLookback, Duration overlap) {
        return dao.findById(syncId)
            .map(meta -> meta.getLastSyncedUntil().minus(overlap))
            .orElse(Instant.now().minus(initialLookback));
    }

    public void updateLastSynced(String syncId, Instant syncedUntil) {
        SyncMetadataDocument meta = dao.findById(syncId)
            .orElse(new SyncMetadataDocument(syncId, null, null));
        meta.setLastSyncedUntil(syncedUntil);
        meta.setLastSyncedAt(Instant.now());
        dao.save(meta);
    }
}
