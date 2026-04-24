package com.atlasmonitor.converter;

public interface BidirectionalConverter<A, B> {

    B convertTo(A source);

    A convertFrom(B source);
}
