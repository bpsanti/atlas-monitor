package com.atlasmonitor.application.model;

public record SlowQueryEfficiencyRatios(
    Double docsExaminedToReturnedRatio,
    Double keysExaminedToReturnedRatio
) {}
