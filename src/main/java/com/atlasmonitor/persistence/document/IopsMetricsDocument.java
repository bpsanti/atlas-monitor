package com.atlasmonitor.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "iops_metrics")
@CompoundIndexes({
    @CompoundIndex(name = "dedup_idx", def = "{'processId': 1, 'start': 1}", unique = true),
    @CompoundIndex(name = "range_idx", def = "{'start': 1, 'end': 1}")
})
public class IopsMetricsDocument {

    @Id
    private String id;
    private String processId;
    private String hostname;
    private String partitionName;
    private String granularity;
    private Instant start;
    private Instant end;
    private List<Instant> roleChanges;

    private MetricSeriesEmbedded read;
    private MetricSeriesEmbedded write;
    private MetricSeriesEmbedded total;
    private MetricSeriesEmbedded maxRead;
    private MetricSeriesEmbedded maxWrite;
    private MetricSeriesEmbedded maxTotal;

    private Instant syncedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricSeriesEmbedded {
        private List<DataPointEmbedded> dataPoints;
        private Instant peakTimestamp;
        private Double peakValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataPointEmbedded {
        private Instant timestamp;
        private Double value;
    }
}
