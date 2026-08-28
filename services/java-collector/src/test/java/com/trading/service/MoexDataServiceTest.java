package com.trading.service;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.trading.model.CandleData;

@ExtendWith(MockitoExtension.class)
public class MoexDataServiceTest {

    @InjectMocks
    private MoexDataService moexDataService;
    
    @Test
    void shouldGetCandels(){
        moexDataService.fetchRecentCandles(null);
    }
}