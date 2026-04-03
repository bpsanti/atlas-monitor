package com.atlasmonitor.api;

import com.atlasmonitor.api.dto.SlowQueryResponse;
import com.atlasmonitor.service.SlowQueryAnalysisService;
import com.atlasmonitor.service.SlowQueryService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/slow-queries")
    public List<SlowQueryResponse> getSlowQueries(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
        @RequestParam(required = false) Long durationMs,
        @RequestParam(required = false) Long minDurationMillis,
        @RequestParam(required = false) Integer nLogs
    ) {
        return slowQueryService.getSlowQueries(since, durationMs, minDurationMillis, nLogs);
    }

    @PostMapping("/slow-queries/analyze")
    public Map<String, String> analyzeSlowQuery(@RequestBody SlowQueryResponse query) {
        String analysis = analysisService.analyze(query);
        return Map.of("analysis", analysis);
    }
}
