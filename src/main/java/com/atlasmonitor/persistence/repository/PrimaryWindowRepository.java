package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.model.PrimaryWindow;
import com.atlasmonitor.persistence.dao.PrimaryWindowDao;
import com.atlasmonitor.persistence.document.PrimaryWindowDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class PrimaryWindowRepository {

    private final PrimaryWindowDao dao;
    private final ConversionService conversionService;

    public List<PrimaryWindow> findOverlapping(Instant start, Instant end) {
        return dao.findByFromLessThanEqualAndUntilGreaterThanEqualOrderByFromAsc(end, start)
            .stream()
            .map(doc -> conversionService.convert(doc, PrimaryWindow.class))
            .toList();
    }

    public void save(PrimaryWindow window) {
        var oldProcessWindow = dao.findTopByOrderByUntilDesc()
            .filter(it -> Objects.equals(it.getProcessId(), window.processId()))
            .filter(it -> isContiguousOrOverlapping(it, window));

        if (oldProcessWindow.isPresent()) {
            var doc = oldProcessWindow.get();

            if (window.until().isAfter(doc.getUntil())) {
                doc.setUntil(window.until());
                doc.setSyncedAt(Instant.now());
                dao.save(doc);
            }
            return;
        }

        PrimaryWindowDocument doc = conversionService.convert(window, PrimaryWindowDocument.class);
        dao.save(doc);
    }

    private boolean isContiguousOrOverlapping(PrimaryWindowDocument existing, PrimaryWindow incoming) {
        return !incoming.from().isAfter(existing.getUntil());
    }
}
