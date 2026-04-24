package com.atlasmonitor.application;

import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasMetricResource;
import com.atlasmonitor.client.resource.AtlasReplicaResource;
import com.atlasmonitor.application.model.PrimaryWindow;
import com.atlasmonitor.persistence.repository.PrimaryWindowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrimaryReplicaResolutionService {
    private final AtlasApiClient atlasApiClient;

    /**
    * Determines which node(s) held the PRIMARY role during [{@code start}, {@code end}]
    * by inspecting {@code OPCOUNTER_INSERT} across all replica set members.
    *
    * <p>Only the primary receives real client inserts, so any node with {@code INSERT > 0}
    * at a given granularity slot is the primary for that slot. Consecutive slots
    * with the same winner are merged into a single {@link PrimaryWindow}.
    *
    * <p>Falls back to the current primary (live process list) when all slots show
    * zero inserts — e.g. a read-only window with no insert activity.
    *
    * @param granularity ISO 8601 duration string (e.g. {@code PT1H}, {@code PT5M})
    * @param start       start of the query window (inclusive)
    * @param end         end of the query window (inclusive)
    * @return ordered list of primary windows covering the requested interval
    */
    public List<PrimaryWindow> resolvePrimaryWindows(String granularity, Instant start, Instant end) {
        var replicas = atlasApiClient.listReplicas().results();
        var primaryWindows = fetchPrimaryWindows(replicas, granularity, start, end);

        if (primaryWindows.isEmpty()) {
            primaryWindows = replicas.stream()
                .filter(p -> p.typeName() != null && p.typeName().contains("PRIMARY"))
                .map(p -> new PrimaryWindow(p.id(), p.hostname(), start, end))
                .toList();
        }

        return primaryWindows;
    }

    /**
    * Walks the replica-by-instant map in chronological order and merges consecutive
    * slots with the same primary into {@link PrimaryWindow} objects.
    *
    * @return an ordered list of primary windows, or an empty list if no insert
    *         activity was found in the given interval
    */
    private List<PrimaryWindow> fetchPrimaryWindows(
        List<AtlasReplicaResource> replicas,
        String granularity,
        Instant start,
        Instant end
    ) {
        var replicaMappedByInstant = fetchReplicaMappedByInstant(replicas, granularity, start, end);

        List<PrimaryWindow> primaryWindows = new ArrayList<>();
        ReplicaReference currentPrimary = null;
        Instant windowStart = start;

        for (var instant : replicaMappedByInstant.keySet()) {
            var instantPrimary = replicaMappedByInstant.get(instant);

            if (currentPrimary == null) {
                currentPrimary = instantPrimary;
            }

            if (!Objects.equals(instantPrimary, currentPrimary)) {
                var newWindow = new PrimaryWindow(currentPrimary.id(), currentPrimary.hostname(), windowStart, instant);
                currentPrimary = instantPrimary;
                windowStart = instant;

                primaryWindows.add(newWindow);
            }
        }

        if (currentPrimary != null) {
            var lastWindow = new PrimaryWindow(currentPrimary.id(), currentPrimary.hostname(), windowStart, end);
            primaryWindows.add(lastWindow);
        }

        return primaryWindows;
    }

    /**
    * Queries {@code OPCOUNTER_INSERT} for every replica over the given window and
    * returns a chronologically sorted map of timestamp → primary replica.
    *
    * <p>Only the primary receives real client inserts, so the first replica reporting
    * {@code INSERT > 0} at each granularity slot is treated as the primary for that slot.
    * {@code putIfAbsent} ensures that if multiple replicas report inserts at the same
    * timestamp (which should not happen in practice), the first one wins.
    *
    * @return a {@link LinkedHashMap} whose iteration order is chronological
    */
    private Map<Instant, ReplicaReference> fetchReplicaMappedByInstant(
        List<AtlasReplicaResource> replicas,
        String granularity,
        Instant start,
        Instant end
    ) {
        Map<Instant, ReplicaReference> primaryByTimestamp = new HashMap<>();

        for (AtlasReplicaResource replica : replicas) {
            var response = atlasApiClient.getWriteOpcounters(
                replica.id(),
                granularity,
                start,
                end
            );

            var ref = new ReplicaReference(replica.id(), replica.hostname());
            for (var writeCounter : response.measurements()) {
                for (var dataPoint : writeCounter.dataPoints()) {
                    if (dataPoint.timestamp() == null || dataPoint.value() == null || dataPoint.value() == 0) {
                        continue;
                    }

                    primaryByTimestamp.putIfAbsent(dataPoint.timestamp(), ref);
                }
            }
        }

        return primaryByTimestamp.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (a, b) -> a,
                LinkedHashMap::new
            ));
    }

    private record ReplicaReference(String id, String hostname) {}
}
