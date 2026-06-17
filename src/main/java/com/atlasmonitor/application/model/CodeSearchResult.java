package com.atlasmonitor.application.model;

public record CodeSearchResult(
    String filePath,
    String repositoryName,
    String codeFragment,
    String htmlUrl
) {}
