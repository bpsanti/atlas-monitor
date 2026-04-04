package com.atlasmonitor.persistence.dao;

import com.atlasmonitor.persistence.document.IopsMetricsDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface IopsMetricsDao extends MongoRepository<IopsMetricsDocument, String> {

    List<IopsMetricsDocument> findByStartLessThanEqualAndEndGreaterThanEqualOrderByStartAsc(
        Instant end,
        Instant start
    );
}
