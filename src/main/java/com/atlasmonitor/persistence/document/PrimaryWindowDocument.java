package com.atlasmonitor.persistence.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "primary_windows")
@CompoundIndexes({
    @CompoundIndex(name = "dedup_idx", def = "{'processId': 1, 'from': 1}", unique = true),
    @CompoundIndex(name = "range_idx", def = "{'from': 1, 'until': 1}")
})
public class PrimaryWindowDocument {

    @Id
    private String id;
    private String processId;
    private String hostname;
    private Instant from;
    private Instant until;
    private Instant syncedAt;
}
