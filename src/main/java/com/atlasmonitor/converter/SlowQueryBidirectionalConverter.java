package com.atlasmonitor.converter;

import com.atlasmonitor.api.resource.SlowQueryResource;
import com.atlasmonitor.api.resource.SlowQueryResource.QueryExecutionResource;
import com.atlasmonitor.application.model.SlowQueryEfficiencyRatios;
import com.atlasmonitor.application.model.SlowQueryExecution;
import com.atlasmonitor.application.model.SlowQueryShape;
import com.atlasmonitor.application.model.SlowQuery;
import org.springframework.stereotype.Component;

@Component
public class SlowQueryBidirectionalConverter implements BidirectionalConverter<SlowQuery, SlowQueryResource> {

    @Override
    public SlowQueryResource convertTo(SlowQuery source) {
        var exec = source.execution();
        var ratios = exec.ratios();

        var executionResource = new QueryExecutionResource(
            exec.keysExaminedCount(),
            exec.docsExaminedCount(),
            exec.docsReturnedCount(),
            exec.hasIndexCoverage(),
            exec.hasSortStage(),
            ratios.docsExaminedToReturnedRatio(),
            ratios.keysExaminedToReturnedRatio(),
            exec.executionDurationMillis(),
            exec.responseLengthBytes(),
            exec.yieldsCount(),
            exec.remoteAddress(),
            exec.isCursorExhausted()
        );

        return new SlowQueryResource(
            source.occurredAt(),
            source.operationType(),
            source.namespace(),
            source.durationMillis(),
            source.shape().planSummary(),
            source.shape().filter(),
            executionResource
        );
    }

    @Override
    public SlowQuery convertFrom(SlowQueryResource source) {
        var shape = new SlowQueryShape(
            source.planSummary(),
            source.filter()
        );

        var exec = source.execution();
        var ratios = new SlowQueryEfficiencyRatios(
            exec.docsExaminedToReturnedRatio(),
            exec.keysExaminedToReturnedRatio()
        );

        var execution = new SlowQueryExecution(
            exec.keysExaminedCount(),
            exec.docsExaminedCount(),
            exec.docsReturnedCount(),
            exec.hasIndexCoverage(),
            exec.hasSortStage(),
            ratios,
            exec.executionDurationMillis(),
            exec.responseLengthBytes(),
            exec.yieldsCount(),
            exec.remoteAddress(),
            exec.isCursorExhausted()
        );

        return new SlowQuery(
            source.occurredAt(),
            source.operationType(),
            source.namespace(),
            source.durationMillis(),
            shape,
            execution
        );
    }
}
