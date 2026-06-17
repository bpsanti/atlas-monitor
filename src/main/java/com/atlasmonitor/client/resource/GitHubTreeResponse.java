package com.atlasmonitor.client.resource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTreeResponse(
    List<TreeItem> tree
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreeItem(
        String path,
        String type
    ) {}
}
