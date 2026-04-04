package com.atlasmonitor.api;

import com.atlasmonitor.api.resource.SlowQueryAnalysisResource;
import com.atlasmonitor.api.resource.SlowQueryResource;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.application.model.SlowQueryAnalysis;
import com.atlasmonitor.application.SlowQueryAnalysisService;
import com.atlasmonitor.application.SlowQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SlowQueryController {

    private final SlowQueryService slowQueryService;
    private final SlowQueryAnalysisService analysisService;
    private final ConversionService conversionService;

    @GetMapping("/slow-queries")
    public List<SlowQueryResource> getSlowQueries(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        @RequestParam(required = false) Long minDurationMillis,
        @RequestParam(required = false) Integer nLogs
    ) {
        return slowQueryService.getSlowQueries(startDate, endDate, minDurationMillis, nLogs).stream()
            .map(q -> conversionService.convert(q, SlowQueryResource.class))
            .toList();
    }

    @PostMapping("/slow-queries/analysis")
    public ResponseEntity<SlowQueryAnalysisResource> findAnalysis(@RequestBody SlowQueryResource queryResource) {
        SlowQuery query = conversionService.convert(queryResource, SlowQuery.class);
        return analysisService.findAnalysis(query)
            .map(result -> ResponseEntity.ok(new SlowQueryAnalysisResource(
                result.analysis(), result.planSummary(), result.analyzedAt(), result.cached())))
            .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/slow-queries/analyze")
    public SlowQueryAnalysisResource analyzeSlowQuery(@RequestBody SlowQueryResource queryResource) {
        SlowQuery query = conversionService.convert(queryResource, SlowQuery.class);
        SlowQueryAnalysis result = analysisService.analyze(query);
        return new SlowQueryAnalysisResource(
            result.analysis(),
            result.planSummary(),
            result.analyzedAt(),
            result.cached()
        );
    }
}
