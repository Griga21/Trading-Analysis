package com.trading.service;

import com.trading.customExceptions.DataSaveWriterServiceException;
import com.trading.model.CandleData;
import com.trading.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DataWriterService {

    private final CandleRepository candleRepository;

    public DataWriterService(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    @Transactional
    public void saveCandles(List<CandleData> candles) {
        if (candles == null || candles.isEmpty()) {
            return;
        }

        for (CandleData candle : candles) {
            candleRepository.upsertCandle(
                    candle.getSecurityId(),
                    candle.getTimestamp(),
                    candle.getOpen(),
                    candle.getHigh(),
                    candle.getLow(),
                    candle.getClose(),
                    candle.getVolume());
        }
    }
}