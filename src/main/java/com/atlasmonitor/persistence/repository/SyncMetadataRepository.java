package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.persistence.document.SyncMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncMetadataRepository extends MongoRepository<SyncMetadataDocument, String> {
}
