package com.trading.config;

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
        
        long count = candleRepository.count();
        
        if (count == 0) {
            log.info("Database is empty. Loading data for the last 2 years...");
            loadInitialData();
        } else {
            log.info("Database contains {} records. No need to load data.", count);
        }
    }
    
    private void loadInitialData() {
        String[] securities = securityRepository.findAll().stream()
                .map(Security::getSecurityId)
                .toArray(String[]::new);
        
        String till = LocalDate.now().toString();
        String from = LocalDate.now().minusYears(2).toString();
        
        log.info("Period of loading: {} - {}", from, till);
        
        for (String security : securities) {
            try {
                log.info("Loading data for {}...", security);
                
                List<CandleData> candles = moexDataService.fetchCandles(
                    security, from, till
                );
                
                if (!candles.isEmpty()) {
                    dataWriterService.saveCandles(candles);
                    log.info("{}: loaded {} candles", security, candles.size());
                } else {
                    log.warn("{}: no data available for the specified period", security);
                }
                
            } catch (Exception e) {
                log.error("Error loading {}: {}", security, e.getMessage());
            }
        }
        
        log.info("=== UPDATE INITIAL DATA IS ENDING ===");
    }
}