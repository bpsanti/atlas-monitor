package com.atlasmonitor.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

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
    private String databaseAnalysis;
    private List<CodeAnalysisEntry> codeAnalyses;
    private Instant analyzedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeAnalysisEntry {
        private String filePath;
        private String repositoryName;
        private String htmlUrl;
        private Integer lineNumber;
        private String analysis;
    }
}
