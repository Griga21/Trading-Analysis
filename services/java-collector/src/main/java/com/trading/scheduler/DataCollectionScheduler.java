package com.trading.scheduler;

import com.trading.model.CandleData;
import com.trading.model.Security;
import com.trading.repository.SecurityRepository;
import com.trading.service.DataWriterService;
import com.trading.service.MoexDataService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataCollectionScheduler {

    private final MoexDataService moexDataService;
    private final DataWriterService dataWriterService;
    private static final Logger log = LoggerFactory.getLogger(DataCollectionScheduler.class);
    private final SecurityRepository securityRepository;

    @Scheduled(cron = "0 0 10  * * *")
    public void collectData() {
        log.info("Starting a data collection task from MOEX " + LocalDateTime.now());

        List<Security> securities = securityRepository.findByActiveTrue();

        for (Security security : securities) {
            collectSecurity(security.getSecurityId());
        }

        log.info("Data collection completed");
    }

    private void collectSecurity(String securityId) {
        try {
            List<CandleData> candles = moexDataService.fetchCandles(
                    securityId,
                    String.valueOf(LocalDateTime.now().toLocalDate().minusYears(2)),
                    String.valueOf(LocalDateTime.now().toLocalDate()));

            List<CandleData> validCandles = candles.stream()
                    .filter(candle -> candle.getTimestamp() != null)
                    .filter(candle -> candle.getOpen() != null)
                    .filter(candle -> candle.getHigh() != null)
                    .filter(candle -> candle.getLow() != null)
                    .filter(candle -> candle.getClose() != null)
                    .filter(candle -> candle.getVolume() != null)
                    .toList();

            if (!validCandles.isEmpty()) {
                dataWriterService.saveCandles(validCandles);
                log.info("Collected {} candles for {}", validCandles.size(), securityId);
            }
        } catch (Exception e) {
            log.error("Failed to collect data for {}", securityId, e);
        }
    }
}