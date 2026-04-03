package com.atlasmonitor.converter;

import com.atlasmonitor.api.resource.ProcessNodeResource;
import com.atlasmonitor.application.model.ProcessNode;
import org.springframework.stereotype.Component;

@Component
public class ProcessNodeBidirectionalConverter implements BidirectionalConverter<ProcessNode, ProcessNodeResource> {

    @Override
    public ProcessNodeResource convertTo(ProcessNode source) {
        return new ProcessNodeResource(
            source.id(),
            source.hostname(),
            source.port(),
            source.type(),
            source.replicaSetName()
        );
    }

    @Override
    public ProcessNode convertFrom(ProcessNodeResource source) {
        return new ProcessNode(
            source.id(),
            source.hostname(),
            source.port(),
            source.type(),
            source.replicaSetName()
        );
    }
}
