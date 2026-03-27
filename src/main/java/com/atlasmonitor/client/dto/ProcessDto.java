package com.atlasmonitor.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessDto(
        String id,
        String hostname,
        int port,
        String typeName,
        String replicaSetName,
        String shardName,
        String version
) {}
