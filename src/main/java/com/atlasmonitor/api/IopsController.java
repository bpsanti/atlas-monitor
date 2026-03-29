package com.atlasmonitor.api;

import com.atlasmonitor.api.dto.IopsPeakResponse;
import com.atlasmonitor.api.dto.IopsQueryResponse;
import com.atlasmonitor.api.dto.ProcessInfo;
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
     */
    @GetMapping("/processes")
    public List<ProcessInfo> listProcesses() {
        return iopsService.listProcesses();
    }

    /**
     * List disk partitions available for a given process.
     */
    @GetMapping("/processes/{processId}/disks")
    public List<String> listDisks(@PathVariable String processId) {
        return iopsService.listDisks(processId);
    }

    /**
     * Query IOPS metrics for all instances in the time window.
     *
     * @param granularity ISO 8601 duration: PT1M, PT5M, PT1H, P1D
     * @param start       Start of time window (ISO 8601)
     * @param end         End of time window (ISO 8601)
     */
    @GetMapping("/iops")
    public List<IopsQueryResponse> queryIops(
            @RequestParam(defaultValue = "PT1H") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return iopsService.queryIops(granularity, start, end);
    }

    /**
     * Query IOPS metrics for the PRIMARY node only.
     * Handles failover: returns one entry per primary window when a role change occurred.
     *
     * @param granularity ISO 8601 duration: PT1M, PT5M, PT1H, P1D
     * @param start       Start of time window (ISO 8601)
     * @param end         End of time window (ISO 8601)
     */
    @GetMapping("/iops/primary")
    public IopsQueryResponse queryPrimaryIops(
            @RequestParam(defaultValue = "PT1H") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return iopsService.queryPrimaryIops(granularity, start, end);
    }

    /**
     * Returns only the peak value for each IOPS metric on the PRIMARY node (no time-series data).
     *
     * @param granularity ISO 8601 duration: PT1M, PT5M, PT1H, P1D
     * @param start       Start of time window (ISO 8601)
     * @param end         End of time window (ISO 8601)
     */
    @GetMapping("/iops/primary/peak")
    public IopsPeakResponse queryPrimaryIopsPeak(
            @RequestParam(defaultValue = "PT1H") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return iopsService.queryPrimaryIopsPeak(granularity, start, end);
    }
}
