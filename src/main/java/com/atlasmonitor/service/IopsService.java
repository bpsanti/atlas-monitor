package com.atlasmonitor.service;

import com.atlasmonitor.api.dto.IopsPeakResponse;
import com.atlasmonitor.api.dto.IopsQueryResponse;
import com.atlasmonitor.api.dto.IopsQueryResponse.MetricSummary;
import com.atlasmonitor.api.dto.IopsQueryResponse.PeakPoint;
import com.atlasmonitor.api.dto.ProcessInfo;
import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.dto.AtlasDataPointResource;
import com.atlasmonitor.client.dto.AtlasMetricResourceResource;
import com.atlasmonitor.client.dto.AtlasMetricResourceWrapperResource;
import com.atlasmonitor.client.dto.AtlasReplicaResource;
import com.atlasmonitor.model.NodeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IopsService {

    private final AtlasApiClient atlasApiClient;
    private final PrimaryReplicaResolutionService primaryResolutionService;

    public List<ProcessInfo> listProcesses() {
        return atlasApiClient.listReplicas().results().stream()
                .map(p -> new ProcessInfo(p.id(), p.hostname(), p.port(), p.typeName(), p.replicaSetName()))
                .toList();
    }

    public List<String> listDisks(String processId) {
        return atlasApiClient.listDisks(processId).results().stream()
                .map(d -> d.partitionName())
                .toList();
    }

    public List<IopsQueryResponse> queryIops(
            NodeType nodeType,
            String granularity,
            Instant start,
            Instant end
    ) {
        if (nodeType == NodeType.PRIMARY) {
            return primaryResolutionService.resolvePrimaryWindows(granularity, start, end).stream()
                    .map(w -> queryIopsForNode(w.processId(), w.hostname(), "REPLICA_PRIMARY",
                            granularity, w.from(), w.until()))
                    .toList();
        }

        return resolveProcesses(nodeType).stream()
                .map(p -> queryIopsForNode(p.id(), p.hostname(), p.typeName(), granularity, start, end))
                .toList();
    }

    public List<IopsPeakResponse> queryIopsPeaks(
            NodeType nodeType,
            String granularity,
            Instant start,
            Instant end
    ) {
        return queryIops(nodeType, granularity, start, end).stream()
                .map(full -> new IopsPeakResponse(
                        full.processId(),
                        full.hostname(),
                        full.currentRole(),
                        full.partitionName(),
                        full.granularity(),
                        full.start(),
                        full.end(),
                        peakOf(full.read()),
                        peakOf(full.write()),
                        peakOf(full.total()),
                        peakOf(full.maxRead()),
                        peakOf(full.maxWrite()),
                        peakOf(full.maxTotal())
                ))
                .toList();
    }

    private IopsQueryResponse queryIopsForNode(
            String processId,
            String hostname,
            String role,
            String granularity,
            Instant start,
            Instant end
    ) {
        String partitionName = resolvePartitionName(processId);
        AtlasMetricResourceWrapperResource raw = atlasApiClient.getDiskIops(processId, partitionName, granularity, start, end);

        Map<String, AtlasMetricResource> byName = raw.measurements().stream()
                .collect(Collectors.toMap(AtlasMetricResource::name, m -> m));

        return new IopsQueryResponse(
                processId,
                hostname,
                role,
                partitionName,
                granularity,
                raw.start(),
                raw.end(),
                summarize(byName.get("DISK_PARTITION_IOPS_READ")),
                summarize(byName.get("DISK_PARTITION_IOPS_WRITE")),
                summarize(byName.get("DISK_PARTITION_IOPS_TOTAL")),
                summarize(byName.get("MAX_DISK_PARTITION_IOPS_READ")),
                summarize(byName.get("MAX_DISK_PARTITION_IOPS_WRITE")),
                summarize(byName.get("MAX_DISK_PARTITION_IOPS_TOTAL"))
        );
    }

    private List<AtlasReplicaResource> resolveProcesses(NodeType nodeType) {
        List<AtlasReplicaResource> matches = atlasApiClient.listReplicas().results().stream()
                .filter(p -> nodeType.matches(p.typeName()))
                .toList();
        if (matches.isEmpty()) {
            throw new NoSuchElementException("No processes found with node type: " + nodeType);
        }
        return matches;
    }

    private String resolvePartitionName(String processId) {
        return atlasApiClient.listDisks(processId).results().stream()
                .map(d -> d.partitionName())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No disk partition found for process: " + processId));
    }

    private PeakPoint peakOf(MetricSummary summary) {
        return summary != null ? summary.peak() : null;
    }

    private MetricSummary summarize(AtlasMetricResource measurement) {
        if (measurement == null) {
            return null;
        }

        List<AtlasDataPointResource> nonNull = measurement.dataPoints().stream()
                .filter(dp -> dp.value() != null)
                .toList();

        if (nonNull.isEmpty()) {
            return new MetricSummary(measurement.dataPoints(), null);
        }

        AtlasDataPointResource peak = nonNull.stream()
                .max(Comparator.comparingDouble(AtlasDataPointResource::value))
                .orElseThrow();

        return new MetricSummary(
                measurement.dataPoints(),
                new PeakPoint(peak.timestamp(), peak.value())
        );
    }
}
