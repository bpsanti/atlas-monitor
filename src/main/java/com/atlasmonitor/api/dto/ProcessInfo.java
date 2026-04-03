package com.atlasmonitor.api.dto;

public record ProcessInfo(
    String id,
    String hostname,
    int port,
    String type,
    String replicaSetName
) {}
