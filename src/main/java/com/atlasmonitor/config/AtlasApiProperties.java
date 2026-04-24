package com.atlasmonitor.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "atlas")
public record AtlasApiProperties(
    @NotBlank String baseUrl,
    @NotBlank String publicKey,
    @NotBlank String privateKey,
    @NotBlank String groupId
) {}
