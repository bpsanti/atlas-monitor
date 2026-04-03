package com.atlasmonitor.converter;

import com.atlasmonitor.client.resource.AtlasDataPointResource;
import com.atlasmonitor.client.resource.AtlasMetricResource;
import com.atlasmonitor.application.model.DataPoint;
import com.atlasmonitor.application.model.MetricSeries;
import com.atlasmonitor.application.model.Peak;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AtlasMetricToMetricSeriesConverter implements Converter<AtlasMetricResource, MetricSeries> {

    @Override
    public MetricSeries convert(AtlasMetricResource source) {
        List<DataPoint> dataPoints = source.dataPoints().stream()
            .map(dp -> new DataPoint(dp.timestamp(), dp.value()))
            .toList();

        List<DataPoint> nonNull = dataPoints.stream()
            .filter(dp -> dp.value() != null)
            .toList();

        if (nonNull.isEmpty()) {
            return new MetricSeries(dataPoints, null);
        }

        DataPoint peakPoint = nonNull.stream()
            .max(Comparator.comparingDouble(DataPoint::value))
            .orElseThrow();

        return new MetricSeries(dataPoints, new Peak(peakPoint.timestamp(), peakPoint.value()));
    }
}
