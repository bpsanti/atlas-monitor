package com.atlasmonitor.service;

import com.atlasmonitor.api.dto.IopsPeakResponse;
import com.atlasmonitor.api.dto.IopsQueryResponse;
import com.atlasmonitor.api.dto.IopsQueryResponse.MetricSummary;
import com.atlasmonitor.api.dto.IopsQueryResponse.PeakPoint;
import com.atlasmonitor.api.dto.ProcessInfo;
import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasDataPointResource;
import com.atlasmonitor.client.resource.AtlasMetricResource;
import com.atlasmonitor.client.resource.AtlasMetricWrapperResource;
import com.atlasmonitor.client.resource.AtlasReplicaResource;
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
        String granularity,
        Instant start,
        Instant end
    ) {
        return atlasApiClient.listReplicas().results().stream()
            .map(p -> queryIopsForNode(p.id(), p.hostname(), p.typeName(), granularity, start, end))
            .toList();
    }

    public IopsQueryResponse queryPrimaryIops(
        String granularity,
        Instant start,
        Instant end
    ) {
        List<IopsQueryResponse> windows = primaryResolutionService.resolvePrimaryWindows(granularity, start, end)
            .stream()
            .map(w -> queryIopsForNode(w.processId(), w.hostname(), "REPLICA_PRIMARY",
                granularity, w.from(), w.until()))
            .toList();

        if (windows.isEmpty()) {
            throw new NoSuchElementException("No primary windows found for the given time range");
        }
        if (windows.size() == 1) {
            return windows.get(0);
        }
        return mergeWindows(windows, granularity, start, end);
    }

    public IopsPeakResponse queryPrimaryIopsPeak(
        String granularity,
        Instant start,
        Instant end
    ) {
        IopsQueryResponse full = queryPrimaryIops(granularity, start, end);
        return new IopsPeakResponse(
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
        );
    }

    private IopsQueryResponse mergeWindows(
        List<IopsQueryResponse> windows,
        String granularity,
        Instant start,
        Instant end
    ) {
        String processIds = windows.stream().map(IopsQueryResponse::processId).distinct()
            .collect(Collectors.joining(", "));
        String hostnames = windows.stream().map(IopsQueryResponse::hostname).distinct()
            .collect(Collectors.joining(", "));

        List<Instant> roleChanges = windows.stream().skip(1).map(IopsQueryResponse::start).toList();

        return new IopsQueryResponse(
            processIds,
            hostnames,
            "REPLICA_PRIMARY",
            windows.get(0).partitionName(),
            granularity,
            start,
            end,
            roleChanges,
            mergeMetrics(windows.stream().map(IopsQueryResponse::read).toList()),
            mergeMetrics(windows.stream().map(IopsQueryResponse::write).toList()),
            mergeMetrics(windows.stream().map(IopsQueryResponse::total).toList()),
            mergeMetrics(windows.stream().map(IopsQueryResponse::maxRead).toList()),
            mergeMetrics(windows.stream().map(IopsQueryResponse::maxWrite).toList()),
            mergeMetrics(windows.stream().map(IopsQueryResponse::maxTotal).toList())
        );
    }

    private MetricSummary mergeMetrics(List<MetricSummary> summaries) {
        List<AtlasDataPointResource> allPoints = summaries.stream()
            .filter(s -> s != null)
            .flatMap(s -> s.dataPoints().stream())
            .sorted(Comparator.comparing(AtlasDataPointResource::timestamp,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        List<AtlasDataPointResource> nonNull = allPoints.stream()
            .filter(dp -> dp.value() != null)
            .toList();

        if (nonNull.isEmpty()) {
            return new MetricSummary(allPoints, null);
        }

        AtlasDataPointResource peak = nonNull.stream()
            .max(Comparator.comparingDouble(AtlasDataPointResource::value))
            .orElseThrow();

        return new MetricSummary(allPoints, new PeakPoint(peak.timestamp(), peak.value()));
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
        AtlasMetricWrapperResource raw = atlasApiClient.getDiskIops(processId, partitionName, granularity, start, end);

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
            List.of(),
            summarize(byName.get("DISK_PARTITION_IOPS_READ")),
            summarize(byName.get("DISK_PARTITION_IOPS_WRITE")),
            summarize(byName.get("DISK_PARTITION_IOPS_TOTAL")),
            summarize(byName.get("MAX_DISK_PARTITION_IOPS_READ")),
            summarize(byName.get("MAX_DISK_PARTITION_IOPS_WRITE")),
            summarize(byName.get("MAX_DISK_PARTITION_IOPS_TOTAL"))
        );
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
