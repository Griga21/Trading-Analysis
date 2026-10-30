package com.trading.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.trading.model.CandleData;

public record CandlePageResponse(
        List<CandleResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public static CandlePageResponse from(Page<CandleData> candles) {
        return new CandlePageResponse(
                candles.getContent().stream().map(CandleResponse::from).toList(),
                candles.getNumber(),
                candles.getSize(),
                candles.getTotalElements(),
                candles.getTotalPages());
    }
}
