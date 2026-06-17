package com.atlasmonitor.application.model;

public record CodeAnalysis(
    String filePath,
    String repositoryName,
    String htmlUrl,
    Integer lineNumber,
    String analysis
) {}
