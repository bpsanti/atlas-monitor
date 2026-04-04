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
        doc.setDate(source.occurredAt());
        doc.setType(source.operationType());
        doc.setNamespace(source.namespace());
        doc.setDurationMillis(source.durationMillis());
        doc.setPlanSummary(source.shape().planSummary());
        doc.setFilter(source.shape().filter());

        var exec = source.execution();
        doc.setKeysExamined(exec.keysExaminedCount());
        doc.setDocsExamined(exec.docsExaminedCount());
        doc.setNreturned(exec.docsReturnedCount());
        doc.setHasIndexCoverage(exec.hasIndexCoverage());
        doc.setHasSort(exec.hasSortStage());
        doc.setOperationExecutionTime(exec.executionDurationMillis());
        doc.setResponseLength(exec.responseLengthBytes());
        doc.setNumYields(exec.yieldsCount());
        doc.setRemote(exec.remoteAddress());
        doc.setCursorExhausted(exec.isCursorExhausted());

        var ratios = exec.ratios();
        doc.setDocsExaminedReturnedRatio(ratios.docsExaminedToReturnedRatio());
        doc.setKeysExaminedReturnedRatio(ratios.keysExaminedToReturnedRatio());

        doc.setSyncedAt(Instant.now());
        return doc;
    }

    @Override
    public SlowQuery convertFrom(SlowQueryDocument source) {
        var shape = new SlowQueryShape(
            source.getPlanSummary(),
            source.getFilter()
        );

        var ratios = new SlowQueryEfficiencyRatios(
            source.getDocsExaminedReturnedRatio(),
            source.getKeysExaminedReturnedRatio()
        );

        var execution = new SlowQueryExecution(
            source.getKeysExamined(),
            source.getDocsExamined(),
            source.getNreturned(),
            source.getHasIndexCoverage(),
            source.getHasSort(),
            ratios,
            source.getOperationExecutionTime(),
            source.getResponseLength(),
            source.getNumYields(),
            source.getRemote(),
            source.getCursorExhausted()
        );

        return new SlowQuery(
            source.getDate(),
            source.getType(),
            source.getNamespace(),
            source.getDurationMillis(),
            shape,
            execution
        );
    }
}
