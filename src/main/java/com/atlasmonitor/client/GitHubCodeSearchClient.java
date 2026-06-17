package com.atlasmonitor.client;

import com.atlasmonitor.client.resource.GitHubCodeSearchResponse;
import com.atlasmonitor.client.resource.GitHubFileContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import com.atlasmonitor.client.resource.GitHubTreeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class GitHubCodeSearchClient {

    private final RestClient githubRestClient;
    private final AtomicBoolean searchRateLimited = new AtomicBoolean(false);

    public GitHubCodeSearchClient(@Qualifier("githubRestClient") RestClient githubRestClient) {
        this.githubRestClient = githubRestClient;
    }

    public boolean isSearchRateLimited() {
        return searchRateLimited.get();
    }

    public void resetSearchRateLimit() {
        searchRateLimited.set(false);
    }

    public GitHubCodeSearchResponse searchCode(String query) {
        if (searchRateLimited.get()) {
            return emptySearch();
        }

        try {
            var response = githubRestClient.get()
                .uri(b -> b.path("/search/code")
                    .queryParam("q", query)
                    .queryParam("per_page", 5)
                    .build())
                .retrieve()
                .body(GitHubCodeSearchResponse.class);

            return response != null ? response : emptySearch();
        } catch (RestClientException e) {
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                log.warn("GitHub code search rate limited, stopping further searches");
                searchRateLimited.set(true);
            } else {
                log.warn("GitHub code search failed for query '{}': {}", query, e.getMessage());
            }
            return emptySearch();
        }
    }

    public String readFileContent(String repoFullName, String filePath) {
        try {
            var response = githubRestClient.get()
                .uri("/repos/" + repoFullName + "/contents/" + filePath)
                .retrieve()
                .body(GitHubFileContentResponse.class);

            if (response == null || response.content() == null) {
                return "Error: File not found or empty";
            }

            var cleanedBase64 = response.content().replaceAll("\\s", "");
            return new String(Base64.getDecoder().decode(cleanedBase64), StandardCharsets.UTF_8);
        } catch (RestClientException e) {
            log.warn("GitHub file read failed for {}/{}: {}", repoFullName, filePath, e.getMessage());
            return "Error reading file: " + e.getMessage();
        }
    }

    public List<String> getRepositoryTree(String repoFullName) {
        try {
            var response = githubRestClient.get()
                .uri("/repos/" + repoFullName + "/git/trees/HEAD?recursive=1")
                .retrieve()
                .body(GitHubTreeResponse.class);

            if (response == null || response.tree() == null) {
                return List.of();
            }

            return response.tree().stream()
                .filter(item -> "blob".equals(item.type()))
                .map(GitHubTreeResponse.TreeItem::path)
                .filter(path -> path.endsWith(".java"))
                .toList();
        } catch (RestClientException e) {
            log.warn("GitHub tree fetch failed for {}: {}", repoFullName, e.getMessage());
            return List.of();
        }
    }

    private GitHubCodeSearchResponse emptySearch() {
        return new GitHubCodeSearchResponse(0, List.of());
    }
}
