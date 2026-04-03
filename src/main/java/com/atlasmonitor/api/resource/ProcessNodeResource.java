package com.atlasmonitor.api.resource;

public record ProcessNodeResource(
    String id,
    String hostname,
    int port,
    String type,
    String replicaSetName
) {}
