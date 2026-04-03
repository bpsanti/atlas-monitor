package com.atlasmonitor.application.model;

public record ProcessNode(
    String id,
    String hostname,
    int port,
    String type,
    String replicaSetName
) {}
