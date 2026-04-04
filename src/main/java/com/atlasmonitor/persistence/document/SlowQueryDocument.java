package com.atlasmonitor.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "slow_queries")
@CompoundIndexes({
    @CompoundIndex(name = "dedup_idx", def = "{'occurredAt': 1, 'namespace': 1, 'durationMillis': 1, 'queryFilter': 1}", unique = true),
    @CompoundIndex(name = "namespace_date_idx", def = "{'namespace': 1, 'occurredAt': 1}")
})
public class SlowQueryDocument {

    @Id
    private String id;

    @Indexed
    private Instant occurredAt;
    private String operationType;
    private String namespace;
    private long durationMillis;
    private String planSummary;
    private Long keysExaminedCount;
    private Long docsExaminedCount;
    private Long docsReturnedCount;
    private Double docsExaminedToReturnedRatio;
    private Double keysExaminedToReturnedRatio;
    private Boolean hasIndexCoverage;
    private Boolean hasSortStage;
    private Long executionDurationMillis;
    private Long responseLengthBytes;
    private Long yieldsCount;
    private String remoteAddress;
    private Boolean isCursorExhausted;
    private String queryFilter;

    private String processId;
    private Instant syncedAt;
}
