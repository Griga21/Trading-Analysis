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
import java.time.temporal.ChronoUnit;
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
            log.warn(
                    "No active securities found in the database. Please add securities to the database before running the application.");
            return;
        }

        for (String security : securities) {
            Optional<CandleData> latestCandle = candleRepository.findFirstBySecurityIdOrderByTimestampDesc(security);

            LocalDate from = latestCandle
                    .map(candle -> candle.getTimestamp().toLocalDate())
                    .orElse(LocalDate.now().minusYears(10));

            long daysMissing = ChronoUnit.DAYS.between(from, LocalDate.now());

            if (daysMissing == 0) {
                log.info("Database contains up-to-date data for {}. No need to load data.", security);
                continue;
            }

            if (latestCandle.isPresent()) {
                log.info("Latest candle for {} is from {}. Loading {} missing days.", security, from, daysMissing);
            } else {
                log.info("No candle data found for {}. Loading data for the last 10 years.", security);
            }
            loadInitialDataByPeriod(security, from.toString(), LocalDate.now().toString());
        }

        log.info("=== INITIAL DATA CHECK COMPLETED ===");

    }

    private void loadInitialDataByPeriod(String security, String from, String till) {
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