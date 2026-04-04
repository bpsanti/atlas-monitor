package com.atlasmonitor.persistence.repository;

import com.atlasmonitor.application.model.SlowQuery;
import com.atlasmonitor.persistence.dao.SlowQueryDao;
import com.atlasmonitor.persistence.document.SlowQueryDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SlowQueryRepository {

    private final SlowQueryDao dao;
    private final ConversionService conversionService;

    public List<SlowQuery> findByDateRange(Instant from, Instant until, long minDurationMillis) {
        return dao.findByOccurredAtBetweenAndDurationMillisGreaterThanEqualOrderByOccurredAtDesc(from, until, minDurationMillis)
            .stream()
            .map(doc -> conversionService.convert(doc, SlowQuery.class))
            .toList();
    }

    public boolean insertIfAbsent(SlowQuery query, String processId) {
        SlowQueryDocument doc = conversionService.convert(query, SlowQueryDocument.class);
        doc.setProcessId(processId);
        try {
            dao.insert(doc);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }
}
