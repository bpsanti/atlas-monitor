package com.atlasmonitor.converter;

import com.atlasmonitor.api.resource.SlowQueryResource;
import com.atlasmonitor.application.model.SlowQuery;
import org.springframework.stereotype.Component;

@Component
public class SlowQueryBidirectionalConverter implements BidirectionalConverter<SlowQuery, SlowQueryResource> {

    @Override
    public SlowQueryResource convertTo(SlowQuery source) {
        return new SlowQueryResource(
            source.date(),
            source.type(),
            source.namespace(),
            source.durationMillis(),
            source.planSummary(),
            source.keysExamined(),
            source.docsExamined(),
            source.nreturned(),
            source.docsExaminedReturnedRatio(),
            source.keysExaminedReturnedRatio(),
            source.hasIndexCoverage(),
            source.hasSort(),
            source.operationExecutionTime(),
            source.responseLength(),
            source.numYields(),
            source.remote(),
            source.cursorExhausted(),
            source.filter()
        );
    }

    @Override
    public SlowQuery convertFrom(SlowQueryResource source) {
        return new SlowQuery(
            source.date(),
            source.type(),
            source.namespace(),
            source.durationMillis(),
            source.planSummary(),
            source.keysExamined(),
            source.docsExamined(),
            source.nreturned(),
            source.docsExaminedReturnedRatio(),
            source.keysExaminedReturnedRatio(),
            source.hasIndexCoverage(),
            source.hasSort(),
            source.operationExecutionTime(),
            source.responseLength(),
            source.numYields(),
            source.remote(),
            source.cursorExhausted(),
            source.filter()
        );
    }
}
