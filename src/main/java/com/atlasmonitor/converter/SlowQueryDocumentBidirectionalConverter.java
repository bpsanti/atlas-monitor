package com.atlasmonitor.converter;

import com.atlasmonitor.application.model.SlowQueryEfficiencyRatios;
import com.atlasmonitor.application.model.SlowQueryExecution;
import com.atlasmonitor.application.model.SlowQueryShape;
import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.persistence.document.SlowQueryDocument;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SlowQueryDocumentBidirectionalConverter implements BidirectionalConverter<SlowQuery, SlowQueryDocument> {

    @Override
    public SlowQueryDocument convertTo(SlowQuery source) {
        var doc = new SlowQueryDocument();
        doc.setOccurredAt(source.occurredAt());
        doc.setOperationType(source.operationType());
        doc.setNamespace(source.namespace());
        doc.setDurationMillis(source.durationMillis());
        doc.setPlanSummary(source.shape().planSummary());
        doc.setQueryFilter(source.shape().filter());

        var exec = source.execution();
        doc.setKeysExaminedCount(exec.keysExaminedCount());
        doc.setDocsExaminedCount(exec.docsExaminedCount());
        doc.setDocsReturnedCount(exec.docsReturnedCount());
        doc.setHasIndexCoverage(exec.hasIndexCoverage());
        doc.setHasSortStage(exec.hasSortStage());
        doc.setExecutionDurationMillis(exec.executionDurationMillis());
        doc.setResponseLengthBytes(exec.responseLengthBytes());
        doc.setYieldsCount(exec.yieldsCount());
        doc.setRemoteAddress(exec.remoteAddress());
        doc.setIsCursorExhausted(exec.isCursorExhausted());

        var ratios = exec.ratios();
        doc.setDocsExaminedToReturnedRatio(ratios.docsExaminedToReturnedRatio());
        doc.setKeysExaminedToReturnedRatio(ratios.keysExaminedToReturnedRatio());

        doc.setSyncedAt(Instant.now());
        return doc;
    }

    @Override
    public SlowQuery convertFrom(SlowQueryDocument source) {
        var shape = new SlowQueryShape(
            source.getPlanSummary(),
            source.getQueryFilter()
        );

        var ratios = new SlowQueryEfficiencyRatios(
            source.getDocsExaminedToReturnedRatio(),
            source.getKeysExaminedToReturnedRatio()
        );

        var execution = new SlowQueryExecution(
            source.getKeysExaminedCount(),
            source.getDocsExaminedCount(),
            source.getDocsReturnedCount(),
            source.getHasIndexCoverage(),
            source.getHasSortStage(),
            ratios,
            source.getExecutionDurationMillis(),
            source.getResponseLengthBytes(),
            source.getYieldsCount(),
            source.getRemoteAddress(),
            source.getIsCursorExhausted()
        );

        return new SlowQuery(
            source.getOccurredAt(),
            source.getOperationType(),
            source.getNamespace(),
            source.getDurationMillis(),
            shape,
            execution
        );
    }
}
