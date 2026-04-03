package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtlasDiskWrapperResource(
    List<AtlasDiskResource> results,
    int totalCount
) {}
