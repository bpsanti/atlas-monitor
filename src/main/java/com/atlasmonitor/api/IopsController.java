package com.atlasmonitor.api;

import com.atlasmonitor.api.resource.IopsMetricsResource;
import com.atlasmonitor.api.resource.IopsPeakResource;
import com.atlasmonitor.api.resource.ProcessNodeResource;
import com.atlasmonitor.application.IopsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class IopsController {

    private final IopsService iopsService;
    private final ConversionService conversionService;

    @GetMapping("/processes")
    public List<ProcessNodeResource> listProcesses() {
        return iopsService.listProcesses().stream()
            .map(p -> conversionService.convert(p, ProcessNodeResource.class))
            .toList();
    }

    @GetMapping("/processes/{processId}/disks")
    public List<String> listDisks(@PathVariable String processId) {
        return iopsService.listDisks(processId);
    }

    @GetMapping("/iops")
    public List<IopsMetricsResource> queryIops(
        @RequestParam(defaultValue = "PT1H") String granularity,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return iopsService.queryIops(granularity, start, end).stream()
            .map(m -> conversionService.convert(m, IopsMetricsResource.class))
            .toList();
    }

    @GetMapping("/iops/primary")
    public IopsMetricsResource queryPrimaryIops(
        @RequestParam(defaultValue = "PT1H") String granularity,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return conversionService.convert(
            iopsService.queryPrimaryIops(granularity, start, end),
            IopsMetricsResource.class);
    }

    @GetMapping("/iops/primary/peak")
    public IopsPeakResource queryPrimaryIopsPeak(
        @RequestParam(defaultValue = "PT1H") String granularity,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end
    ) {
        return conversionService.convert(
            iopsService.queryPrimaryIopsPeak(granularity, start, end),
            IopsPeakResource.class);
    }
}
