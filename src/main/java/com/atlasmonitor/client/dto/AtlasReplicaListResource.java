package com.atlasmonitor.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasReplicaListResource(
        List<AtlasReplicaResource> results,
        int totalCount
) {}
