package com.trading.scheduler;

import com.trading.model.CandleData;
import com.trading.service.DataWriterService;
import com.trading.service.MoexDataService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataCollectionScheduler {

    private final MoexDataService moexDataService;
    private final DataWriterService dataWriterService;
    private static final Logger log = LoggerFactory.getLogger(DataCollectionScheduler.class);


    @Scheduled(cron = "0 58 10  * * *")
    public void collectData() {
        log.info("Starting a data collection task from MOEX " + LocalDateTime.now());
        String[] securities = { "SBER", "GAZP", "LKOH", "ROSN" };

        for (String security : securities) {
            try {
                List<CandleData> allCandles = moexDataService.fetchCandles(
                        security,
                        String.valueOf(LocalDateTime.now().toLocalDate().minusYears(2)),
                        String.valueOf(LocalDateTime.now().toLocalDate()));

                List<CandleData> filteredCandles = allCandles.stream()
                        .filter(c -> c.getTimestamp() != null)
                        .filter(c -> c.getTimestamp().getYear() >= 2023)
                        .collect(Collectors.toList());

                if (!filteredCandles.isEmpty()) {
                    log.info("Received data for {}: {}", security, filteredCandles.size());
                    dataWriterService.saveCandles(filteredCandles);
                }
            } catch (Exception e) {
                log.info("Error receiving data for {}: {}", security, e.getMessage());
            }
            log.info("The survey is completed" + LocalDateTime.now());
        }
    }
}