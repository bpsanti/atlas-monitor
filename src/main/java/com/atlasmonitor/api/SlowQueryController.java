package com.atlasmonitor.api;

import com.atlasmonitor.api.resource.SlowQueryResource;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.application.SlowQueryAnalysisService;
import com.atlasmonitor.application.SlowQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SlowQueryController {

    private final SlowQueryService slowQueryService;
    private final SlowQueryAnalysisService analysisService;
    private final ConversionService conversionService;

    @GetMapping("/slow-queries")
    public List<SlowQueryResource> getSlowQueries(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
        @RequestParam(required = false) Long durationMs,
        @RequestParam(required = false) Long minDurationMillis,
        @RequestParam(required = false) Integer nLogs
    ) {
        return slowQueryService.getSlowQueries(since, durationMs, minDurationMillis, nLogs).stream()
            .map(q -> conversionService.convert(q, SlowQueryResource.class))
            .toList();
    }

    @PostMapping("/slow-queries/analyze")
    public Map<String, String> analyzeSlowQuery(@RequestBody SlowQueryResource queryResource) {
        SlowQuery query = conversionService.convert(queryResource, SlowQuery.class);
        String analysis = analysisService.analyze(query);
        return Map.of("analysis", analysis);
    }
}
