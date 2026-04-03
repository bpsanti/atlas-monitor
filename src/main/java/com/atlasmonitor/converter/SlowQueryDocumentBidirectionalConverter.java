package com.atlasmonitor.converter;

import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.persistence.document.SlowQueryDocument;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SlowQueryDocumentBidirectionalConverter implements BidirectionalConverter<SlowQuery, SlowQueryDocument> {

    @Override
    public SlowQueryDocument convertTo(SlowQuery source) {
        var doc = new SlowQueryDocument();
        doc.setDate(source.date());
        doc.setType(source.type());
        doc.setNamespace(source.namespace());
        doc.setDurationMillis(source.durationMillis());
        doc.setPlanSummary(source.planSummary());
        doc.setKeysExamined(source.keysExamined());
        doc.setDocsExamined(source.docsExamined());
        doc.setNreturned(source.nreturned());
        doc.setDocsExaminedReturnedRatio(source.docsExaminedReturnedRatio());
        doc.setKeysExaminedReturnedRatio(source.keysExaminedReturnedRatio());
        doc.setHasIndexCoverage(source.hasIndexCoverage());
        doc.setHasSort(source.hasSort());
        doc.setOperationExecutionTime(source.operationExecutionTime());
        doc.setResponseLength(source.responseLength());
        doc.setNumYields(source.numYields());
        doc.setRemote(source.remote());
        doc.setCursorExhausted(source.cursorExhausted());
        doc.setFilter(source.filter());
        doc.setSyncedAt(Instant.now());
        return doc;
    }

    @Override
    public SlowQuery convertFrom(SlowQueryDocument source) {
        return new SlowQuery(
            source.getDate(),
            source.getType(),
            source.getNamespace(),
            source.getDurationMillis(),
            source.getPlanSummary(),
            source.getKeysExamined(),
            source.getDocsExamined(),
            source.getNreturned(),
            source.getDocsExaminedReturnedRatio(),
            source.getKeysExaminedReturnedRatio(),
            source.getHasIndexCoverage(),
            source.getHasSort(),
            source.getOperationExecutionTime(),
            source.getResponseLength(),
            source.getNumYields(),
            source.getRemote(),
            source.getCursorExhausted(),
            source.getFilter()
        );
    }
}
