package com.atlasmonitor.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiskListResponse(
        List<DiskDto> results,
        int totalCount
) {}
