package com.trading.config;

import com.trading.model.CandleData;
import com.trading.repository.CandleRepository;
import com.trading.service.DataWriterService;
import com.trading.service.MoexDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private final CandleRepository candleRepository;
    private final MoexDataService moexDataService;
    private final DataWriterService dataWriterService;
    
    public DataInitializer(
            CandleRepository candleRepository,
            MoexDataService moexDataService,
            DataWriterService dataWriterService) {
        this.candleRepository = candleRepository;
        this.moexDataService = moexDataService;
        this.dataWriterService = dataWriterService;
    }
    
    @Override
    public void run(String... args) {
        log.info("=== ПРОВЕРКА БАЗЫ ДАННЫХ ===");
        
        long count = candleRepository.count();
        
        if (count == 0) {
            log.info("База данных пуста. Загружаем данные за последние 2 года...");
            loadInitialData();
        } else {
            log.info("База данных содержит {} записей. Загрузка не требуется.", count);
        }
    }
    
    private void loadInitialData() {
        String[] securities = {"SBER", "GAZP", "LKOH", "ROSN"};
        
        String till = LocalDate.now().toString();
        String from = LocalDate.now().minusYears(2).toString();
        
        log.info("Период загрузки: {} - {}", from, till);
        
        for (String security : securities) {
            try {
                log.info("Загрузка данных для {}...", security);
                
                List<CandleData> candles = moexDataService.fetchCandles(
                    security, from, till
                );
                
                if (!candles.isEmpty()) {
                    dataWriterService.saveCandles(candles);
                    log.info("{}: загружено {} свечей", security, candles.size());
                } else {
                    log.warn("{}: нет данных за указанный период", security);
                }
                
            } catch (Exception e) {
                log.error("Ошибка загрузки {}: {}", security, e.getMessage());
            }
        }
        
        log.info("=== НАЧАЛЬНАЯ ЗАГРУЗКА ЗАВЕРШЕНА ===");
    }
}