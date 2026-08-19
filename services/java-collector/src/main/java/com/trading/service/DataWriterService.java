package com.trading.service;

import com.trading.model.CandleData;
import com.trading.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.LoggerFactory;
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
        
        int saved = 0;
        int skipped = 0;
        
        for (CandleData candle : candles) {
            try {
                boolean exists = candleRepository.existsBySecurityIdAndTimestamp(
                    candle.getSecurityId(),
                    candle.getTimestamp()
                );
                
                if (!exists) {
                    candleRepository.save(candle);
                    saved++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                skipped++;
            }
        }
    }
}