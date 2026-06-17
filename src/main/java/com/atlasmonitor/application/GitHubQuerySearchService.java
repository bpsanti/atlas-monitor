package com.atlasmonitor.application;

import com.atlasmonitor.application.model.CodeSearchResult;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.client.GitHubCodeSearchClient;
import com.atlasmonitor.config.GitHubProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubQuerySearchService {

    private final GitHubCodeSearchClient gitHubCodeSearchClient;
    private final GitHubProperties gitHubProperties;

    public List<CodeSearchResult> searchSourceCode(SlowQuery query) {
        if (gitHubProperties.repositories() == null || gitHubProperties.repositories().isEmpty()) {
            return List.of();
        }

        var collectionName = extractCollectionName(query.namespace());
        var entityName = deriveEntityName(collectionName);
        var filterFields = extractFilterFields(query.shape().filter());

        var resultsByPath = new LinkedHashMap<String, CodeSearchResult>();

        for (var repo : gitHubProperties.repositories()) {
            searchRepository(repo, entityName, collectionName, filterFields, resultsByPath);
        }

        return new ArrayList<>(resultsByPath.values());
    }

    private void searchRepository(String repo, String entityName, String collectionName,
                                  Set<String> filterFields, LinkedHashMap<String, CodeSearchResult> resultsByPath) {

        var allVariants = buildFieldVariants(filterFields);

        // Search 1: all field variants together (most precise — finds files containing all filter fields)
        if (allVariants.size() >= 2) {
            for (var combination : buildFieldCombinationQueries(repo, allVariants)) {
                addResults(combination, resultsByPath);
            }
        }

        // Search 2: MongoDB field names with collection name (Criteria/MongoTemplate style)
        if (!filterFields.isEmpty()) {
            var fieldTerms = filterFields.stream()
                .limit(3)
                .map(f -> "\"" + f + "\"")
                .collect(Collectors.joining(" "));
            addResults(String.format("repo:%s language:java \"%s\" %s", repo, collectionName, fieldTerms), resultsByPath);
        }

        // Search 3: entity Predicates class — for predicate-to-field mapping context
        addResults(String.format("repo:%s language:java \"%sPredicates\"", repo, entityName), resultsByPath);
    }

    /**
     * For each MongoDB field name, generates search variants:
     *   is_deleted -> ["is_deleted", "isDeleted", "deleted"]
     *   farmId -> ["farmId"]
     *   date -> ["date"]
     * Returns the shortest/most unique variant for each field to use in search.
     */
    private Set<String> buildFieldVariants(Set<String> filterFields) {
        return filterFields.stream()
            .flatMap(field -> {
                var variants = new ArrayList<String>();
                variants.add(field);
                // Add camelCase variant
                if (field.contains("_")) {
                    variants.add(toCamelCase(field));
                    // Add the meaningful part (is_deleted -> deleted)
                    if (field.startsWith("is_")) {
                        variants.add(field.substring(3));
                        variants.add(toCamelCase(field.substring(3)));
                    }
                }
                return variants.stream();
            })
            .collect(Collectors.toSet());
    }

    /**
     * Builds multiple search queries using different combinations of field variants.
     * GitHub requires all quoted terms to be present in the file, but they can be anywhere.
     * We use one search term per original filter field, picking the best variant.
     */
    private List<String> buildFieldCombinationQueries(String repo, Set<String> allVariants) {
        var queries = new ArrayList<String>();

        // Use all variants together — GitHub matches files containing ALL terms
        var allTerms = allVariants.stream()
            .map(f -> "\"" + f + "\"")
            .collect(Collectors.joining(" "));
        queries.add(String.format("repo:%s language:java %s", repo, allTerms));

        return queries;
    }

    /**
     * Converts snake_case to camelCase.
     * Examples: is_deleted -> isDeleted, farm_id -> farmId
     */
    private String toCamelCase(String snakeCase) {
        if (!snakeCase.contains("_")) {
            return snakeCase;
        }
        var parts = snakeCase.split("_");
        var sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(parts[i].substring(0, 1).toUpperCase())
                  .append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
     * Derives a likely Java entity/class name from a MongoDB collection name.
     * Examples:
     *   T_BILL -> Bill
     *   bills -> Bill
     *   user_accounts -> UserAccount
     *   T_FARM_SEASON -> FarmSeason
     */
    String deriveEntityName(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            return "";
        }

        var name = collectionName.replaceFirst("^(?i)(T_|V_|TBL_)", "");

        if (name.contains("_")) {
            return Arrays.stream(name.split("_"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase())
                .collect(Collectors.joining());
        }

        var result = name.substring(0, 1).toUpperCase() + name.substring(1);
        if (result.length() > 3 && result.endsWith("s") && !result.endsWith("ss")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private void addResults(String query, LinkedHashMap<String, CodeSearchResult> resultsByPath) {
        log.debug("GitHub code search: {}", query);
        var response = gitHubCodeSearchClient.searchCode(query);

        for (var item : response.items()) {
            var key = item.repository().fullName() + ":" + item.path();
            if (resultsByPath.containsKey(key)) {
                continue;
            }

            // Only include files that are likely query-building code
            var pathLower = item.path().toLowerCase();
            if (!isQueryRelatedFile(pathLower)) {
                log.debug("Skipping non-query file: {}", item.path());
                continue;
            }

            var fragment = item.textMatches() != null && !item.textMatches().isEmpty()
                ? item.textMatches().stream()
                    .map(m -> m.fragment())
                    .collect(Collectors.joining("\n...\n"))
                : "";

            if (fragment.length() > 800) {
                fragment = fragment.substring(0, 800) + "...";
            }

            resultsByPath.put(key, new CodeSearchResult(
                item.path(),
                item.repository().fullName(),
                fragment,
                item.htmlUrl()
            ));
        }
    }

    private boolean isQueryRelatedFile(String path) {
        return path.contains("predicate")
            || path.contains("repository")
            || path.contains("dao")
            || path.contains("service")
            || path.contains("query")
            || path.contains("criteria")
            || path.contains("specification")
            || path.contains("filter")
            || path.contains("handler")
            || path.contains("controller")
            || path.contains("impl");
    }

    private String extractCollectionName(String namespace) {
        if (namespace == null) {
            return "";
        }
        var parts = namespace.split("\\.");
        return parts.length > 1 ? parts[parts.length - 1] : namespace;
    }

    private Set<String> extractFilterFields(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) {
            return Set.of();
        }

        try {
            var trimmed = filterJson.trim();
            if (trimmed.startsWith("[")) {
                trimmed = "{\"p\":" + trimmed + "}";
            }
            var doc = Document.parse(trimmed);
            return extractKeys(doc).stream()
                .filter(key -> !key.startsWith("$") && !key.equals("p"))
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("Failed to extract filter fields from: {}", filterJson, e);
            return Set.of();
        }
    }

    private List<String> extractKeys(Document doc) {
        var keys = new ArrayList<String>();
        for (var entry : doc.entrySet()) {
            keys.add(entry.getKey());
            if (entry.getValue() instanceof Document nested) {
                keys.addAll(extractKeys(nested));
            } else if (entry.getValue() instanceof List<?> list) {
                for (var item : list) {
                    if (item instanceof Document nested) {
                        keys.addAll(extractKeys(nested));
                    }
                }
            }
        }
        return keys;
    }
}
