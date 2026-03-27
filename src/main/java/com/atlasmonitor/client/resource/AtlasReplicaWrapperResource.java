package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasReplicaWrapperResource(
        List<AtlasReplicaResource> results,
        int totalCount
) {}
