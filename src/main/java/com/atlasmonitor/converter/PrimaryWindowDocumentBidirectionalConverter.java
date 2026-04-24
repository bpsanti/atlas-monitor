package com.atlasmonitor.converter;

import com.atlasmonitor.application.model.PrimaryWindow;
import com.atlasmonitor.persistence.document.PrimaryWindowDocument;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PrimaryWindowDocumentBidirectionalConverter implements BidirectionalConverter<PrimaryWindow, PrimaryWindowDocument> {

    @Override
    public PrimaryWindowDocument convertTo(PrimaryWindow source) {
        var doc = new PrimaryWindowDocument();
        doc.setProcessId(source.processId());
        doc.setHostname(source.hostname());
        doc.setFrom(source.from());
        doc.setUntil(source.until());
        doc.setSyncedAt(Instant.now());
        return doc;
    }

    @Override
    public PrimaryWindow convertFrom(PrimaryWindowDocument source) {
        return new PrimaryWindow(
            source.getProcessId(),
            source.getHostname(),
            source.getFrom(),
            source.getUntil()
        );
    }
}
