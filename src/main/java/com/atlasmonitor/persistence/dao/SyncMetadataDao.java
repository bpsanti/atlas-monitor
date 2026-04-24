package com.atlasmonitor.persistence.dao;

import com.atlasmonitor.persistence.document.SyncMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyncMetadataDao extends MongoRepository<SyncMetadataDocument, String> {
}
