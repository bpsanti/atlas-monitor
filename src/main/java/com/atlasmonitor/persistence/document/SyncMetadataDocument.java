package com.atlasmonitor.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sync_metadata")
public class SyncMetadataDocument {

    @Id
    private String id;
    private Instant lastSyncedUntil;
    private Instant lastSyncedAt;
}
