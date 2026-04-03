package com.atlasmonitor.converter;

import com.atlasmonitor.client.resource.AtlasReplicaResource;
import com.atlasmonitor.application.model.ProcessNode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AtlasReplicaToProcessNodeConverter implements Converter<AtlasReplicaResource, ProcessNode> {

    @Override
    public ProcessNode convert(AtlasReplicaResource source) {
        return new ProcessNode(
            source.id(),
            source.hostname(),
            source.port(),
            source.typeName(),
            source.replicaSetName()
        );
    }
}
