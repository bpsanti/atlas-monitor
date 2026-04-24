package com.atlasmonitor.converter;

import com.atlasmonitor.client.resource.AtlasMetricResource;
import com.atlasmonitor.application.model.IopsDataPoint;
import com.atlasmonitor.application.model.IopsMetricSeries;
import com.atlasmonitor.application.model.IopsMetricPeak;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AtlasMetricToMetricSeriesConverter implements Converter<AtlasMetricResource, IopsMetricSeries> {

    @Override
    public IopsMetricSeries convert(AtlasMetricResource source) {
        List<IopsDataPoint> dataPoints = source.dataPoints().stream()
            .map(dp -> new IopsDataPoint(dp.timestamp(), dp.value()))
            .toList();

        List<IopsDataPoint> nonNull = dataPoints.stream()
            .filter(dp -> dp.value() != null)
            .toList();

        if (nonNull.isEmpty()) {
            return new IopsMetricSeries(dataPoints, null);
        }

        IopsDataPoint peakPoint = nonNull.stream()
            .max(Comparator.comparingDouble(IopsDataPoint::value))
            .orElseThrow();

        return new IopsMetricSeries(dataPoints, new IopsMetricPeak(peakPoint.timestamp(), peakPoint.value()));
    }
}
