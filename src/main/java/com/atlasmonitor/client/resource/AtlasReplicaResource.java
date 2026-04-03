package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasReplicaResource(
    String id,
    String hostname,
    int port,
    String typeName,
    String replicaSetName,
    String shardName,
    String version
) {}
