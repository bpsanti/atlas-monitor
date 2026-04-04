package com.atlasmonitor.client;

import com.atlasmonitor.client.resource.AtlasDiskWrapperResource;
import com.atlasmonitor.client.resource.AtlasMetricWrapperResource;
import com.atlasmonitor.client.resource.AtlasReplicaWrapperResource;
import com.atlasmonitor.client.resource.AtlasSlowQueryWrapperResource;
import com.atlasmonitor.config.AtlasApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AtlasApiClient {

    private final RestClient atlasRestClient;
    private final AtlasApiProperties props;

    public AtlasReplicaWrapperResource listReplicas() {
        return atlasRestClient.get()
            .uri("/groups/{groupId}/processes", props.groupId())
            .retrieve()
            .body(AtlasReplicaWrapperResource.class);
    }

    public AtlasDiskWrapperResource listDisks(String processId) {
        // processId contains ":" (e.g. host:27017) — build the path as a plain string
        // so Spring does NOT apply URI template encoding (%3A) to it.
        String path = "/groups/" + props.groupId() + "/processes/" + processId + "/disks";
        return atlasRestClient.get()
            .uri(path)
            .retrieve()
            .body(AtlasDiskWrapperResource.class);
    }

    /**
    * Fetches disk partition IOPS measurements from Atlas.
    */
    public AtlasMetricWrapperResource getDiskIops(
        String processId,
        String partitionName,
        String granularity,
        Instant start,
        Instant end
    ) {
        String basePath = "/groups/" + props.groupId()
            + "/processes/" + processId
            + "/disks/" + partitionName
            + "/measurements";

        return atlasRestClient.get()
            .uri(b -> b.path(basePath)
                .queryParam("granularity", granularity)
                .queryParam("start", start.toString())
                .queryParam("end", end.toString())
                .queryParam("m",
                    "DISK_PARTITION_IOPS_READ",
                    "DISK_PARTITION_IOPS_WRITE",
                    "DISK_PARTITION_IOPS_TOTAL",
                    "MAX_DISK_PARTITION_IOPS_READ",
                    "MAX_DISK_PARTITION_IOPS_WRITE",
                    "MAX_DISK_PARTITION_IOPS_TOTAL")
                .build())
            .retrieve()
            .body(AtlasMetricWrapperResource.class);
    }

    /**
    * Fetches process-level write opcounters (INSERT + UPDATE + DELETE) for a node.
    * Used to identify which node was acting as primary during a given window —
    * only the primary receives real client writes.
    */
    public AtlasMetricWrapperResource getWriteOpcounters(
        String processId,
        String granularity,
        Instant start,
        Instant end
    ) {
        String basePath = "/groups/" + props.groupId()
            + "/processes/" + processId
            + "/measurements";

        return atlasRestClient.get()
            .uri(b -> b.path(basePath)
                .queryParam("granularity", granularity)
                .queryParam("start", start.toString())
                .queryParam("end", end.toString())
                .queryParam("m", "OPCOUNTER_INSERT")
                .build())
            .retrieve()
            .body(AtlasMetricWrapperResource.class);
    }

    public AtlasSlowQueryWrapperResource getSlowQueryLogs(
        String processId,
        Instant startDate,
        Instant endDate
    ) {
        String basePath = "/groups/" + props.groupId()
            + "/processes/" + processId
            + "/performanceAdvisor/slowQueryLogs";

        Long since = startDate != null ? startDate.toEpochMilli() : null;
        Long duration = startDate != null && endDate != null
            ? endDate.toEpochMilli() - startDate.toEpochMilli()
            : null;

        return atlasRestClient.get()
            .uri(b -> {
                b.path(basePath)
                    .queryParam("includeMetrics", true)
                    .queryParam("includeReplicaState", true)
                    .queryParam("includeOpType", true);
                if (since != null) {
                    b.queryParam("since", since);
                }
                if (duration != null) {
                    b.queryParam("duration", duration);
                }
                return b.build();
            })
            .retrieve()
            .body(AtlasSlowQueryWrapperResource.class);
    }
}
