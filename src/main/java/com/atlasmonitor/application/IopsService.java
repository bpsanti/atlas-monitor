package com.atlasmonitor.application;

import com.atlasmonitor.client.AtlasApiClient;
import com.atlasmonitor.client.resource.AtlasMetricResource;
import com.atlasmonitor.client.resource.AtlasMetricWrapperResource;
import com.atlasmonitor.application.model.DataPoint;
import com.atlasmonitor.application.model.IopsMetrics;
import com.atlasmonitor.application.model.IopsPeak;
import com.atlasmonitor.application.model.MetricSeries;
import com.atlasmonitor.application.model.Peak;
import com.atlasmonitor.application.model.ProcessNode;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
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
    private final ConversionService conversionService;

    public List<ProcessNode> listProcesses() {
        return atlasApiClient.listReplicas().results().stream()
            .map(p -> conversionService.convert(p, ProcessNode.class))
            .toList();
    }

    public List<String> listDisks(String processId) {
        return atlasApiClient.listDisks(processId).results().stream()
            .map(d -> d.partitionName())
            .toList();
    }

    public List<IopsMetrics> queryIops(
        String granularity,
        Instant start,
        Instant end
    ) {
        return atlasApiClient.listReplicas().results().stream()
            .map(p -> queryIopsForNode(p.id(), p.hostname(), p.typeName(), granularity, start, end))
            .toList();
    }

    public IopsMetrics queryPrimaryIops(
        String granularity,
        Instant start,
        Instant end
    ) {
        List<IopsMetrics> windows = primaryResolutionService.resolvePrimaryWindows(granularity, start, end)
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

    public IopsPeak queryPrimaryIopsPeak(
        String granularity,
        Instant start,
        Instant end
    ) {
        IopsMetrics full = queryPrimaryIops(granularity, start, end);
        return new IopsPeak(
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

    private IopsMetrics mergeWindows(
        List<IopsMetrics> windows,
        String granularity,
        Instant start,
        Instant end
    ) {
        String processIds = windows.stream().map(IopsMetrics::processId).distinct()
            .collect(Collectors.joining(", "));
        String hostnames = windows.stream().map(IopsMetrics::hostname).distinct()
            .collect(Collectors.joining(", "));

        List<Instant> roleChanges = windows.stream().skip(1).map(IopsMetrics::start).toList();

        return new IopsMetrics(
            processIds,
            hostnames,
            "REPLICA_PRIMARY",
            windows.get(0).partitionName(),
            granularity,
            start,
            end,
            roleChanges,
            mergeMetrics(windows.stream().map(IopsMetrics::read).toList()),
            mergeMetrics(windows.stream().map(IopsMetrics::write).toList()),
            mergeMetrics(windows.stream().map(IopsMetrics::total).toList()),
            mergeMetrics(windows.stream().map(IopsMetrics::maxRead).toList()),
            mergeMetrics(windows.stream().map(IopsMetrics::maxWrite).toList()),
            mergeMetrics(windows.stream().map(IopsMetrics::maxTotal).toList())
        );
    }

    private MetricSeries mergeMetrics(List<MetricSeries> seriesList) {
        List<DataPoint> allPoints = seriesList.stream()
            .filter(s -> s != null)
            .flatMap(s -> s.dataPoints().stream())
            .sorted(Comparator.comparing(DataPoint::timestamp,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        List<DataPoint> nonNull = allPoints.stream()
            .filter(dp -> dp.value() != null)
            .toList();

        if (nonNull.isEmpty()) {
            return new MetricSeries(allPoints, null);
        }

        DataPoint peakPoint = nonNull.stream()
            .max(Comparator.comparingDouble(DataPoint::value))
            .orElseThrow();

        return new MetricSeries(allPoints, new Peak(peakPoint.timestamp(), peakPoint.value()));
    }

    private IopsMetrics queryIopsForNode(
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

        return new IopsMetrics(
            processId,
            hostname,
            role,
            partitionName,
            granularity,
            raw.start(),
            raw.end(),
            List.of(),
            convertMetric(byName.get("DISK_PARTITION_IOPS_READ")),
            convertMetric(byName.get("DISK_PARTITION_IOPS_WRITE")),
            convertMetric(byName.get("DISK_PARTITION_IOPS_TOTAL")),
            convertMetric(byName.get("MAX_DISK_PARTITION_IOPS_READ")),
            convertMetric(byName.get("MAX_DISK_PARTITION_IOPS_WRITE")),
            convertMetric(byName.get("MAX_DISK_PARTITION_IOPS_TOTAL"))
        );
    }

    private String resolvePartitionName(String processId) {
        return atlasApiClient.listDisks(processId).results().stream()
            .map(d -> d.partitionName())
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(
                "No disk partition found for process: " + processId));
    }

    private Peak peakOf(MetricSeries series) {
        return series != null ? series.peak() : null;
    }

    private MetricSeries convertMetric(AtlasMetricResource metric) {
        return metric != null ? conversionService.convert(metric, MetricSeries.class) : null;
    }
}
