package com.atlasmonitor.service;

import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.dto.DataPointDto;
import com.atlasmonitor.client.dto.MeasurementDto;
import com.atlasmonitor.client.dto.MeasurementsResponse;
import com.atlasmonitor.client.dto.ProcessDto;
import com.atlasmonitor.model.NodeType;
import com.atlasmonitor.model.PrimaryWindow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PrimaryResolutionService {

    private final AtlasApiClient atlasApiClient;

    /**
     * Determines which node(s) held the PRIMARY role during [start, end]
     * by inspecting OPCOUNTER_INSERT across all replica set members.
     *
     * Only the primary receives real client inserts, so any node with INSERT > 0
     * at a given granularity slot is the primary for that slot. Consecutive slots
     * with the same winner are merged into a single PrimaryWindow.
     *
     * Falls back to the current primary (live process list) when all slots show
     * zero inserts — e.g. a read-only window with no insert activity.
     */
    public List<PrimaryWindow> resolvePrimaryWindows(String granularity, Instant start, Instant end) {
        List<ProcessDto> allProcesses = atlasApiClient.listProcesses().results();

        // Fetch OPCOUNTER_INSERT per node and map each to its timestamps
        Map<String, Map<Instant, Double>> insertsByNode = new HashMap<>();
        Map<String, String> idToHostname = new HashMap<>();

        for (ProcessDto process : allProcesses) {
            idToHostname.put(process.id(), process.hostname());

            MeasurementsResponse response = atlasApiClient.getWriteOpcounters(
                    process.id(), granularity, start, end);

            Map<Instant, Double> insertCounts = new HashMap<>();
            for (MeasurementDto m : response.measurements()) {
                for (DataPointDto dp : m.dataPoints()) {
                    if (dp.timestamp() != null && dp.value() != null) {
                        insertCounts.put(dp.timestamp(), dp.value());
                    }
                }
            }
            insertsByNode.put(process.id(), insertCounts);
        }

        // Walk timestamps in order; the primary at each slot is the node with INSERT > 0
        List<Instant> timestamps = insertsByNode.values().stream()
                .flatMap(m -> m.keySet().stream())
                .distinct()
                .sorted()
                .toList();

        List<PrimaryWindow> windows = new ArrayList<>();
        String currentPrimary = null;
        Instant windowStart = start;

        for (Instant ts : timestamps) {
            String winner = insertsByNode.entrySet().stream()
                    .filter(e -> e.getValue().getOrDefault(ts, 0.0) > 0)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (winner == null) continue; // no inserts this slot — primary unchanged

            if (!Objects.equals(winner, currentPrimary)) {
                if (currentPrimary != null) {
                    windows.add(new PrimaryWindow(
                            currentPrimary, idToHostname.get(currentPrimary), windowStart, ts));
                }
                currentPrimary = winner;
                windowStart = ts;
            }
        }

        if (currentPrimary != null) {
            windows.add(new PrimaryWindow(
                    currentPrimary, idToHostname.get(currentPrimary), windowStart, end));
            return windows;
        }

        // No insert activity found — fall back to the current primary
        return atlasApiClient.listProcesses().results().stream()
                .filter(p -> NodeType.PRIMARY.matches(p.typeName()))
                .map(p -> new PrimaryWindow(p.id(), p.hostname(), start, end))
                .toList();
    }
}
