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
    @CompoundIndex(name = "dedup_idx", def = "{'date': 1, 'namespace': 1, 'durationMillis': 1, 'filter': 1}", unique = true),
    @CompoundIndex(name = "namespace_date_idx", def = "{'namespace': 1, 'date': 1}")
})
public class SlowQueryDocument {

    @Id
    private String id;

    @Indexed
    private Instant date;
    private String type;
    private String namespace;
    private long durationMillis;
    private String planSummary;
    private Long keysExamined;
    private Long docsExamined;
    private Long nreturned;
    private Double docsExaminedReturnedRatio;
    private Double keysExaminedReturnedRatio;
    private Boolean hasIndexCoverage;
    private Boolean hasSort;
    private Long operationExecutionTime;
    private Long responseLength;
    private Long numYields;
    private String remote;
    private Boolean cursorExhausted;
    private String filter;

    private String processId;
    private Instant syncedAt;
}
