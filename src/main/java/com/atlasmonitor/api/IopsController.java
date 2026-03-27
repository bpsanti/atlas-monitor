package com.atlasmonitor.api;

import com.atlasmonitor.api.dto.IopsPeakResponse;
import com.atlasmonitor.api.dto.IopsQueryResponse;
import com.atlasmonitor.api.dto.ProcessInfo;
import com.atlasmonitor.model.NodeType;
import com.atlasmonitor.service.IopsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IopsController {

    private final IopsService iopsService;

    /**
     * List all processes (hosts) in the configured Atlas project.
     * Use the returned "id" field as the processId in subsequent calls.
     */
    @GetMapping("/processes")
    public List<ProcessInfo> listProcesses() {
        return iopsService.listProcesses();
    }

    /**
     * List disk partitions available for a given process.
     * The partition name (e.g. "data") is required when querying IOPS.
     */
    @GetMapping("/processes/{processId}/disks")
    public List<String> listDisks(@PathVariable String processId) {
        return iopsService.listDisks(processId);
    }

    /**
     * Query IOPS metrics for the given node type and time window.
     * The process ID and disk partition are resolved automatically.
     *
     * @param nodeType    PRIMARY or SECONDARY
     * @param granularity ISO 8601 duration: PT1M, PT5M, PT1H, P1D
     * @param start       Start of time window (ISO 8601, e.g. 2024-01-01T00:00:00Z)
     * @param end         End of time window (ISO 8601)
     */
    @GetMapping("/iops")
    public List<IopsQueryResponse> queryIops(
            @RequestParam NodeType nodeType,
            @RequestParam(defaultValue = "PT1H") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return iopsService.queryIops(nodeType, granularity, start, end);
    }

    /**
     * Returns only the peak value (timestamp + value) for each IOPS metric,
     * with no time-series data.
     */
    @GetMapping("/iops/peak")
    public List<IopsPeakResponse> queryIopsPeaks(
            @RequestParam NodeType nodeType,
            @RequestParam(defaultValue = "PT1H") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return iopsService.queryIopsPeaks(nodeType, granularity, start, end);
    }
}
