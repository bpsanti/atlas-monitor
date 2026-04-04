package com.atlasmonitor.application;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class QueryShapeNormalizer {

    private static final JsonWriterSettings JSON_SETTINGS = JsonWriterSettings.builder()
        .outputMode(org.bson.json.JsonMode.RELAXED)
        .build();

    public String computeShapeHash(String namespace, String planSummary, String filter) {
        String normalizedFilter = extractShape(filter);
        String raw = (namespace != null ? namespace : "")
            + "|" + (planSummary != null ? planSummary : "")
            + "|" + (normalizedFilter != null ? normalizedFilter : "");
        return sha256(raw);
    }

    public String extractShape(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) {
            return null;
        }

        var trimmedFilterJson = filterJson.trim();
        if (trimmedFilterJson.startsWith("[")) {
            trimmedFilterJson = "{\"p\":" + trimmedFilterJson + "}";
        }

        var filterDocument = Document.parse(trimmedFilterJson);
        return extractShapeFromDocument(filterDocument).toJson(JSON_SETTINGS);
    }

    private Document extractShapeFromDocument(Document filterDocument) {
        for (var entry : filterDocument.entrySet()) {
            if (entry.getValue() instanceof Document nestedDocument) {
                extractShapeFromDocument(nestedDocument);
                continue;
            }

            if (entry.getValue() instanceof List<?> list) {
                var listShapes = new ArrayList<>();
                for (var element : list) {
                    if (element instanceof Document nestedDocument) {
                        listShapes.add(extractShapeFromDocument(nestedDocument));
                    } else {
                        listShapes.add("?");
                    }
                }
                entry.setValue(listShapes.stream().distinct().toList());
                continue;
            }

            entry.setValue("?");
        }

        return filterDocument;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
