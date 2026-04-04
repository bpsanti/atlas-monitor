package com.atlasmonitor.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "slow_query_analyses")
public class SlowQueryAnalysisDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String shapeHash;

    private String namespace;
    private String planSummary;
    private String normalizedFilter;
    private String analysis;
    private Instant analyzedAt;
}
