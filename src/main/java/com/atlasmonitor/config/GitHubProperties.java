package com.atlasmonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "github")
public record GitHubProperties(
    String token,
    List<String> repositories
) {}
