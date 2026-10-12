package com.trading.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.trading.model.CandleData;
import com.trading.service.interfecies.CandleService;
import com.trading.repository.CandleRepository;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class CandleServiceImp implements CandleService {

    private final CandleRepository candleRepository;

    @Override
    public CandleData getCandleBySecurityId(Long securityId) {
        return candleRepository.findBySecurityIdOrderByTimestampDesc(String.valueOf(securityId)).stream().findFirst().orElse(null);
    }

    @Override
    public List<CandleData> getAllCandles() {
        return candleRepository.findAll();
    }
    
}
