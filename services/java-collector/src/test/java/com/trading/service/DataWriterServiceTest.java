package com.trading.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trading.model.CandleData;
import com.trading.repository.CandleRepository;

@ExtendWith(MockitoExtension.class)
public class DataWriterServiceTest {

    @Mock
    private CandleRepository candleRepository;
    @InjectMocks
    private DataWriterService dataWriterService;

    @Test
    void shouldSaveCandlesSuccessfully() {
        CandleData candle = new CandleData();
        candle.setSecurityId("SBER");
        candle.setTimestamp(LocalDateTime.of(2026, 8, 28, 0, 0));
         when(candleRepository.existsBySecurityIdAndTimestamp(
                "SBER",
                candle.getTimestamp()
        )).thenReturn(false);

        dataWriterService.saveCandles(List.of(candle));

        verify(candleRepository).save(candle);
    }

}
