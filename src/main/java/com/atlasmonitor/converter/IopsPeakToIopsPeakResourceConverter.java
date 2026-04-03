package com.atlasmonitor.converter;

import com.atlasmonitor.api.resource.IopsPeakResource;
import com.atlasmonitor.api.resource.IopsMetricsResource.PeakResource;
import com.atlasmonitor.application.model.IopsPeak;
import com.atlasmonitor.application.model.Peak;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class IopsPeakToIopsPeakResourceConverter implements Converter<IopsPeak, IopsPeakResource> {

    @Override
    public IopsPeakResource convert(IopsPeak source) {
        return new IopsPeakResource(
            source.processId(),
            source.hostname(),
            source.currentRole(),
            source.partitionName(),
            source.granularity(),
            source.start(),
            source.end(),
            toPeakResource(source.read()),
            toPeakResource(source.write()),
            toPeakResource(source.total()),
            toPeakResource(source.maxRead()),
            toPeakResource(source.maxWrite()),
            toPeakResource(source.maxTotal())
        );
    }

    private PeakResource toPeakResource(Peak peak) {
        if (peak == null) {
            return null;
        }
        return new PeakResource(peak.timestamp(), peak.value());
    }
}
