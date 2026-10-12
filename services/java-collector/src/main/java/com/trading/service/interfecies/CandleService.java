package com.trading.service.interfecies;

import java.util.List;

import org.springframework.stereotype.Service;

import com.trading.model.CandleData;

@Service 
public interface CandleService {

    CandleData getCandleBySecurityId(Long securityId);
    List<CandleData> getAllCandles();
}