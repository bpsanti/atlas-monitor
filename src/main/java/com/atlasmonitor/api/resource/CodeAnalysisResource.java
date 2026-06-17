package com.atlasmonitor.api.resource;

public record CodeAnalysisResource(
    String filePath,
    String repositoryName,
    String htmlUrl,
    Integer lineNumber,
    String analysis
) {}
