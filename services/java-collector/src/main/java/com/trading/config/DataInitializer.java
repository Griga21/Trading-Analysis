package com.trading.config;

import com.trading.customExceptions.DataSaveWriterServiceException;
import com.trading.model.CandleData;
import com.trading.model.Security;
import com.trading.repository.CandleRepository;
import com.trading.repository.SecurityRepository;
import com.trading.service.DataWriterService;
import com.trading.service.MoexDataService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CandleRepository candleRepository;
    private final MoexDataService moexDataService;
    private final DataWriterService dataWriterService;
    private final SecurityRepository securityRepository;

    @Override
    public void run(String... args) {
        log.info("=== CHECKING DATABASE ===");

        List<String> securities = securityRepository.findByActiveTrue()
                .stream()
                .map(Security::getSecurityId)
                .toList();

        if (securities.isEmpty()) {
            log.warn("No active securities found in the database.");
            return;
        }

        for (String security : securities) {
            if (candleRepository.existsBySecurityId(security)) {
                log.info("Database contains data for {}. No need to load data.", security);
            } else {
                log.info("No candle data for {}. Loading the last 2 years...", security);
                loadInitialData(security);
            }
        }

        log.info("=== INITIAL DATA CHECK COMPLETED ===");
    }

    private void loadInitialData(String security) {
        LocalDate today = LocalDate.now();

        Optional<CandleData> latestCandle = candleRepository.findFirstBySecurityIdOrderByTimestampDesc(security);

        String from = latestCandle
                .map(candle -> candle.getTimestamp()
                        .toLocalDate()
                        .minusDays(1))
                .orElse(today.minusYears(2))
                .toString();

        String till = today.toString();

        try {
            log.info("Loading data for {} from {} to {}", security, from, till);

            List<CandleData> candles = moexDataService.fetchCandles(security, from, till);

            if (!candles.isEmpty()) {
                dataWriterService.saveCandles(candles);
                log.info("{}: loaded {} candles", security, candles.size());
            } else {
                log.warn("{}: no data available for {}", security, security);
            }
        } catch (DataSaveWriterServiceException e) {
            log.error("Error loading data for {}", security, e);
        }
    }
}