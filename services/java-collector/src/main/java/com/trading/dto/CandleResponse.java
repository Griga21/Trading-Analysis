package com.trading.dto;

import java.time.LocalDateTime;

import com.trading.model.CandleData;

public record CandleResponse(
        String ticker,
        LocalDateTime timestamp,
        Double open,
        Double high,
        Double low,
        Double close,
        Double volume) {

    public static CandleResponse from(CandleData candle) {
        return new CandleResponse(
                candle.getSecurityId(),
                candle.getTimestamp(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume());
    }
}
